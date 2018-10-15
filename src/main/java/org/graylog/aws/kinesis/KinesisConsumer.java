package org.graylog.aws.kinesis;

import com.amazonaws.regions.Region;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.InvalidStateException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ShutdownException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ThrottlingException;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessorFactory;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.Worker;
import com.amazonaws.services.kinesis.clientlibrary.types.InitializationInput;
import com.amazonaws.services.kinesis.clientlibrary.types.ProcessRecordsInput;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownInput;
import com.amazonaws.services.kinesis.model.Record;
import com.github.rholder.retry.Attempt;
import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.RetryListener;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import okhttp3.HttpUrl;
import org.graylog.aws.auth.AWSAuthProvider;
import org.graylog.aws.config.AWSPluginConfiguration;
import org.graylog.aws.config.Proxy;
import org.graylog.aws.inputs.transports.KinesisTransport;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.system.NodeId;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

public class KinesisConsumer implements Runnable {
    public static final int MAX_THROTTLE_WAIT_MILLIS = 60000;
    private static final Logger LOG = LoggerFactory.getLogger(KinesisConsumer.class);

    private final Region region;
    private final String kinesisStreamName;
    private final AWSAuthProvider authProvider;
    private final NodeId nodeId;
    private final HttpUrl proxyUrl;
    private final AWSPluginConfiguration awsConfig;
    private final Consumer<byte[]> dataHandler;

    private Worker worker;
    private KinesisTransport transport;

    public KinesisConsumer(String kinesisStreamName,
                           Region region,
                           Consumer<byte[]> dataHandler,
                           AWSPluginConfiguration awsConfig,
                           AWSAuthProvider authProvider,
                           NodeId nodeId,
                           @Nullable HttpUrl proxyUrl,
                           KinesisTransport transport) {
        this.kinesisStreamName = requireNonNull(kinesisStreamName, "kinesisStreamName");
        this.region = requireNonNull(region, "region");
        this.dataHandler = requireNonNull(dataHandler, "dataHandler");
        this.awsConfig = requireNonNull(awsConfig, "awsConfig");
        this.authProvider = requireNonNull(authProvider, "authProvider");
        this.nodeId = requireNonNull(nodeId, "nodeId");
        this.proxyUrl = proxyUrl;
        this.transport = transport;
    }

    // TODO metrics
    public void run() {
        final String workerId = String.format(Locale.ENGLISH, "graylog-node-%s", nodeId.anonymize());
        final KinesisClientLibConfiguration config = new KinesisClientLibConfiguration(
                // The application name needs to be unique per input. Using the same name for two different Kinesis
                // streams will cause trouble with state handling in DynamoDB. (used by the Kinesis client under the
                // hood to keep state)
                String.format(Locale.ENGLISH, "graylog-aws-plugin-%s", kinesisStreamName),
                kinesisStreamName,
                authProvider,
                workerId
        ).withRegionName(region.getName()).withMaxRecords(1);

        // Optional HTTP proxy
        if (awsConfig.proxyEnabled() && proxyUrl != null) {
            config.withCommonClientConfig(Proxy.forAWS(proxyUrl));
        }

        final IRecordProcessorFactory recordProcessorFactory = () -> new IRecordProcessor() {
            private DateTime lastCheckpoint = DateTime.now();

            @Override
            public void initialize(InitializationInput initializationInput) {
                LOG.info("Initializing Kinesis worker for stream <{}>", kinesisStreamName);
            }

            @Override
            public void processRecords(ProcessRecordsInput processRecordsInput) {

                LOG.info("processRecords called. Received {} Kinesis events", processRecordsInput.getRecords().size());

                if (transport.isThrottled()) {
                    // Wait if the input is throttled. The max we can wait is 60 seconds (keeps Kinesis processor thread healthy)
                    LOG.info("[throttled] waiting up to [{}ms] for throttling to clear.", MAX_THROTTLE_WAIT_MILLIS);
                    if (!transport.blockUntilUnthrottled(MAX_THROTTLE_WAIT_MILLIS, TimeUnit.MILLISECONDS)) {
                        LOG.info("Throttling did not clear in [{}]ms. Stopping the Kinesis worker to let the " +
                                 "journal clear out.️ It will start again in 5 five minutes.");
                        worker.shutdown();

                        // Request restart in 5 minutes. Let this thread exit.
                        transport.stoppedDueToThrottling.set(true);
                        return;
                    }

                    LOG.info("[unthrottled] transport will resume processing records.");
                    return;
                }

                for (Record record : processRecordsInput.getRecords()) {
                    try {
                        // Create a read-only view of the data and use a safe method to convert it to a byte array
                        // as documented in Record#getData(). (using ByteBuffer#array() can fail)
                        final ByteBuffer dataBuffer = record.getData().asReadOnlyBuffer();
                        final byte[] dataBytes = new byte[dataBuffer.remaining()];
                        dataBuffer.get(dataBytes);

                        dataHandler.accept(Tools.decompressGzip(dataBytes).getBytes());
                    } catch (Exception e) {
                        LOG.error("Couldn't read Kinesis record from stream <{}>", kinesisStreamName, e);
                    }
                }

                // According to the Kinesis client documentation, we should not checkpoint for every record but
                // rather periodically.
                // TODO: Make interval configurable (global)
                if (lastCheckpoint.plusMinutes(1).isBeforeNow()) {
                    lastCheckpoint = DateTime.now();
                    LOG.debug("Checkpointing stream <{}>", kinesisStreamName);
                    checkpoint(processRecordsInput);
                }
            }

            private void checkpoint(ProcessRecordsInput processRecordsInput) {
                final Retryer<Void> retryer = RetryerBuilder.<Void>newBuilder()
                        .retryIfExceptionOfType(ThrottlingException.class)
                        .withWaitStrategy(WaitStrategies.fixedWait(1, TimeUnit.SECONDS))
                        .withStopStrategy(StopStrategies.stopAfterDelay(10, TimeUnit.MINUTES))
                        .withRetryListener(new RetryListener() {
                            @Override
                            public <V> void onRetry(Attempt<V> attempt) {
                                if (attempt.hasException()) {
                                    LOG.warn("Checkpointing stream <{}> failed, retrying. (attempt {})", kinesisStreamName, attempt.getAttemptNumber());
                                }
                            }
                        })
                        .build();

                try {
                    retryer.call(() -> {
                        try {
                            processRecordsInput.getCheckpointer().checkpoint();
                        } catch (InvalidStateException e) {
                            LOG.error("Couldn't save checkpoint to DynamoDB table used by the Kinesis client library - check database table", e);
                        } catch (ShutdownException e) {
                            LOG.debug("Processor is shutting down, skipping checkpoint");
                        }
                        return null;
                    });
                } catch (ExecutionException e) {
                    LOG.error("Couldn't checkpoint stream <{}>", kinesisStreamName, e);
                } catch (RetryException e) {
                    LOG.error("Checkpoint retry for stream <{}> finally failed", kinesisStreamName, e);
                }
            }

            @Override
            public void shutdown(ShutdownInput shutdownInput) {
                LOG.info("Shutting down Kinesis worker for stream <{}>", kinesisStreamName);
            }
        };

        this.worker = new Worker.Builder()
                .recordProcessorFactory(recordProcessorFactory)
                .config(config)
                .build();

        LOG.info("☀️ Before Run"); // TODO remove
        worker.run();
        LOG.info("🌅️ After Run"); // TODO remove

        // Ask Transport to restart if stopped due to throttling.
        transport.requestRestartWhenUnthrottled();
    }

    public void stop() {
        if (worker != null) {
            worker.shutdown();
        }
    }

}
