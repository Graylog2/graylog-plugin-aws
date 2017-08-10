package org.graylog.aws.kinesis;

import com.amazonaws.regions.Region;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessorFactory;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.Worker;
import com.amazonaws.services.kinesis.clientlibrary.types.InitializationInput;
import com.amazonaws.services.kinesis.clientlibrary.types.ProcessRecordsInput;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownInput;
import com.amazonaws.services.kinesis.model.Record;
import okhttp3.HttpUrl;
import org.graylog.aws.auth.AWSAuthProvider;
import org.graylog.aws.config.AWSPluginConfiguration;
import org.graylog.aws.config.Proxy;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.system.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.Locale;
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

    private Worker worker;

    public KinesisConsumer(String kinesisStreamName,
                           Region region,
                           Consumer<byte[]> dataHandler,
                           AWSPluginConfiguration awsConfig,
                           AWSAuthProvider authProvider,
                           NodeId nodeId,
                           @Nullable HttpUrl proxyUrl) {
        this.kinesisStreamName = requireNonNull(kinesisStreamName, "kinesisStreamName");
        this.region = requireNonNull(region, "region");
        this.dataHandler = requireNonNull(dataHandler, "dataHandler");
        this.awsConfig = requireNonNull(awsConfig, "awsConfig");
        this.authProvider = requireNonNull(authProvider, "authProvider");
        this.nodeId = requireNonNull(nodeId, "nodeId");
        this.proxyUrl = proxyUrl;
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
        ).withRegionName(region.getName());

        // Optional HTTP proxy
        if (awsConfig.proxyEnabled() && proxyUrl != null) {
            config.withCommonClientConfig(Proxy.forAWS(proxyUrl));
        }

        final IRecordProcessorFactory recordProcessorFactory = () -> new IRecordProcessor() {
            @Override
            public void initialize(InitializationInput initializationInput) {
                LOG.info("Initializing Kinesis worker for stream <{}>", kinesisStreamName);
            }

            @Override
            public void processRecords(ProcessRecordsInput processRecordsInput) {
                LOG.debug("Received {} Kinesis events", processRecordsInput.getRecords().size());

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

        worker.run();
    }

    public void stop() {
        if (worker != null) {
            worker.shutdown();
        }
    }

}
