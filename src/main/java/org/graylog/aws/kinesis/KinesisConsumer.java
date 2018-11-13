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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.rholder.retry.Attempt;
import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.RetryListener;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import okhttp3.HttpUrl;
import org.graylog.aws.auth.AWSAuthProvider;
import org.graylog.aws.cloudwatch.CloudWatchLogData;
import org.graylog.aws.cloudwatch.CloudWatchLogEntry;
import org.graylog.aws.config.AWSPluginConfiguration;
import org.graylog.aws.config.Proxy;
import org.graylog.aws.inputs.transports.KinesisTransport;
import org.graylog.aws.inputs.transports.KinesisTransportState;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.system.NodeId;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

public class KinesisConsumer implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(KinesisConsumer.class);

    private final Region region;
    private final String kinesisStreamName;
    private final AWSAuthProvider authProvider;
    private final NodeId nodeId;
    private final HttpUrl proxyUrl;
    private final AWSPluginConfiguration awsConfig;
    private final Consumer<byte[]> dataHandler;
    private final Integer maxThrottledWaitMillis;
    private final Integer recordBatchSize;

    private Worker worker;
    private KinesisTransport transport;
    private final ObjectMapper objectMapper;
    /**
     * Checkpointing must be performed when the KinesisConsumer needs to be shuts down due to sustained throttling.
     * At the time when shutdown occurs, checkpointing might not have happened for a while, so we keep track of the
     * last sequence to checkpoint to. */
    private String lastSuccessfulRecordSequence = null;

    public KinesisConsumer(String kinesisStreamName,
                           Region region,
                           Consumer<byte[]> dataHandler,
                           AWSPluginConfiguration awsConfig,
                           AWSAuthProvider authProvider,
                           NodeId nodeId,
                           @Nullable HttpUrl proxyUrl,
                           KinesisTransport transport,
                           ObjectMapper objectMapper,
                           Integer maxThrottledWaitMillis,
                           Integer recordBatchSize) {
        this.kinesisStreamName = requireNonNull(kinesisStreamName, "kinesisStreamName");
        this.region = requireNonNull(region, "region");
        this.dataHandler = requireNonNull(dataHandler, "dataHandler");
        this.awsConfig = requireNonNull(awsConfig, "awsConfig");
        this.authProvider = requireNonNull(authProvider, "authProvider");
        this.nodeId = requireNonNull(nodeId, "nodeId");
        this.proxyUrl = proxyUrl;
        this.transport = transport;
        this.objectMapper = objectMapper;
        this.recordBatchSize = recordBatchSize;
        this.maxThrottledWaitMillis = maxThrottledWaitMillis;
    }

    // TODO metrics
    public void run() {

        transport.consumerState = KinesisTransportState.STARTING;

        LOG.debug("Max wait millis [{}]", maxThrottledWaitMillis);
        LOG.debug("Record batch size [{}]", recordBatchSize);

        final String workerId = String.format(Locale.ENGLISH, "graylog-node-%s", nodeId.anonymize());

        // The application name needs to be unique per input. Using the same name for two different Kinesis
        // streams will cause trouble with state handling in DynamoDB. (used by the Kinesis client under the
        // hood to keep state)
        final String applicationName = String.format(Locale.ENGLISH, "graylog-aws-plugin-%s", kinesisStreamName);
        KinesisClientLibConfiguration config = new KinesisClientLibConfiguration(applicationName, kinesisStreamName,
                                                                                 authProvider, workerId);
        config.withRegionName(region.getName());

        // Default max records is 10k. This can be overridden from UI.
        if (recordBatchSize != null) {
            config.withMaxRecords(recordBatchSize);
        }

        // Optional HTTP proxy
        if (awsConfig.proxyEnabled() && proxyUrl != null) {
            config.withCommonClientConfig(Proxy.forAWS(proxyUrl));
        }

        final IRecordProcessorFactory recordProcessorFactory = () -> new IRecordProcessor() {
            private DateTime lastCheckpoint = DateTime.now();

            @Override
            public void initialize(InitializationInput initializationInput) {
                LOG.debug("Initializing Kinesis worker for stream <{}>", kinesisStreamName);
                transport.consumerState = KinesisTransportState.RUNNING;
            }

            @Override
            public void processRecords(ProcessRecordsInput processRecordsInput) {

                LOG.info("processRecords called. Received {} Kinesis events", processRecordsInput.getRecords().size() * 1024);

                if (transport.isThrottled()) {
                    LOG.info("[throttled] Waiting up to [{}ms] for throttling to clear.", maxThrottledWaitMillis);
                    if (!transport.blockUntilUnthrottled(maxThrottledWaitMillis, TimeUnit.MILLISECONDS)) {

                        /* Stop the Kinesis consumer when throttling does not clear quickly. The AWS Kinesis client
                         * requires that the worker thread stays healthy and does not take too long to respond.
                         * So, if we need to wait a long time for throttling to clear (eg. more than 1 minute), then the
                         * consumer needs to be shutdown and restarted later once throttling clears. */
                        LOG.info("[throttled] Throttling did not clear in [{}]ms. Stopping the Kinesis worker to let " +
                                 "the throttle clear.️ It will start again automatically once throttling clears.", maxThrottledWaitMillis);

                        // Checkpoint last processed record before shutting down.
                        if (lastSuccessfulRecordSequence != null) {
                            checkpoint(processRecordsInput, lastSuccessfulRecordSequence);
                        }

                        transport.consumerState = KinesisTransportState.STOPPING;
                        worker.shutdown();
                        transport.stoppedDueToThrottling.set(true);
                        return;
                    }

                    LOG.info("[unthrottled] Kinesis consumer will now resume processing records.");
                }

                for (Record record : processRecordsInput.getRecords()) {
                    try {
                        // Create a read-only view of the data and use a safe method to convert it to a byte array
                        // as documented in Record#getData(). (using ByteBuffer#array() can fail)
                        final ByteBuffer dataBuffer = record.getData().asReadOnlyBuffer();
                        final byte[] dataBytes = new byte[dataBuffer.remaining()];
                        dataBuffer.get(dataBytes);

                        // Decompress response.
                        final byte[] bytes = Tools.decompressGzip(dataBytes).getBytes();

                        // Extract messages, so that they can be committed to journal one by one.
                        final CloudWatchLogData data = objectMapper.readValue(bytes, CloudWatchLogData.class);
                        Iterator<CloudWatchLogEntry> iterator =
                                data.logEvents.stream().map(le -> new CloudWatchLogEntry( data.logGroup, data.logStream, le.timestamp, le.message)).iterator();

                        // Push all messages to the Journal.
                        while (iterator.hasNext()) {
                            CloudWatchLogEntry next = iterator.next();
                            dataHandler.accept(objectMapper.writeValueAsBytes(next));
                        }

                        lastSuccessfulRecordSequence = record.getSequenceNumber();
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
                    checkpoint(processRecordsInput, null);
                }
            }

            private void checkpoint(ProcessRecordsInput processRecordsInput, String lastSequence) {
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
                            if (lastSequence != null) {
                                processRecordsInput.getCheckpointer().checkpoint(lastSequence);
                            } else {
                                processRecordsInput.getCheckpointer().checkpoint();
                            }
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

        LOG.debug("Before Kinesis worker runs");
        worker.run();
        transport.consumerState = KinesisTransportState.STOPPED;
        LOG.debug("After Kinesis worker runs");
    }

    public void stop() {
        if (worker != null) {
            worker.shutdown();
        }
    }
}