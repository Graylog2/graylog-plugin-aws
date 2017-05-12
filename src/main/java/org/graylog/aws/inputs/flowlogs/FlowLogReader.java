package org.graylog.aws.inputs.flowlogs;

import com.amazonaws.regions.Region;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessorFactory;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.Worker;
import com.amazonaws.services.kinesis.clientlibrary.types.InitializationInput;
import com.amazonaws.services.kinesis.clientlibrary.types.ProcessRecordsInput;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownInput;
import com.amazonaws.services.kinesis.model.Record;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import org.graylog.aws.auth.AWSAuthProvider;
import org.graylog.aws.config.AWSPluginConfiguration;
import org.graylog.aws.config.Proxy;
import org.graylog.aws.inputs.flowlogs.json.FlowLogKinesisEvent;
import org.graylog.aws.inputs.flowlogs.json.RawFlowLog;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.journal.RawMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowLogReader implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(FlowLogReader.class);

    private final Region region;
    private final ObjectMapper objectMapper;
    private final MessageInput sourceInput;
    private final String kinesisStreamName;
    private final AWSAuthProvider authProvider;
    private final HttpUrl proxyUrl;
    private final AWSPluginConfiguration awsConfig;


    private Worker worker;

    public FlowLogReader(String kinesisStreamName,
                         Region region,
                         MessageInput input,
                         ClusterConfigService configService,
                         AWSAuthProvider authProvider,
                         HttpUrl proxyUrl) {
        this.kinesisStreamName = kinesisStreamName;
        this.region = region;
        this.sourceInput = input;
        this.objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        this.authProvider = authProvider;
        this.proxyUrl = proxyUrl;

        awsConfig = configService.get(AWSPluginConfiguration.class);

        LOG.info("Starting AWS FlowLog reader.");
    }

    // TODO metrics
    public void run() {
        final KinesisClientLibConfiguration config = new KinesisClientLibConfiguration(
                "graylog-aws-plugin",
                kinesisStreamName,
                this.authProvider,
                "graylog-server-master")
                .withRegionName(region.getName());

        // Optional HTTP proxy
        if(awsConfig.proxyEnabled() && this.proxyUrl != null) {
            config.withCommonClientConfig(Proxy.forAWS(this.proxyUrl));
        }

        final IRecordProcessorFactory recordProcessorFactory = () -> new IRecordProcessor() {
            @Override
            public void initialize(InitializationInput initializationInput) {
                LOG.info("Initializing Kinesis worker.");
            }

            @Override
            public void processRecords(ProcessRecordsInput processRecordsInput) {
                for (Record record : processRecordsInput.getRecords()) {
                    try {
                        LOG.debug("Received FlowLog events via Kinesis.");

                        FlowLogKinesisEvent event = objectMapper.readValue(Tools.decompressGzip(record.getData().array()), FlowLogKinesisEvent.class);
                        for(RawFlowLog flowlog : event.logEvents) {
                            String fullMessage = flowlog.timestamp + " " + flowlog.message;
                            sourceInput.processRawMessage(new RawMessage(fullMessage.getBytes()));
                        }
                    } catch(Exception e) {
                        LOG.error("Could not read FlowLog record from Kinesis stream.", e);
                    }
                }
            }

            @Override
            public void shutdown(ShutdownInput shutdownInput) {
                LOG.info("Shutting down Kinesis worker.");
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
