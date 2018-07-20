package org.graylog.aws;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.aws.inputs.cloudtrail.CloudTrailCodec;
import org.graylog.aws.inputs.cloudtrail.CloudTrailInput;
import org.graylog.aws.inputs.cloudtrail.CloudTrailTransport;
import org.graylog.aws.inputs.cloudwatch.CloudWatchLogsInput;
import org.graylog.aws.inputs.codecs.CloudWatchFlowLogCodec;
import org.graylog.aws.inputs.codecs.CloudWatchRawLogCodec;
import org.graylog.aws.inputs.flowlogs.FlowLogsInput;
import org.graylog.aws.inputs.transports.KinesisTransport;
import org.graylog.aws.processors.instancelookup.AWSInstanceNameLookupProcessor;
import org.graylog.aws.processors.instancelookup.InstanceLookupTable;
import org.graylog2.plugin.PluginModule;

public class AWSModule extends PluginModule {
    @Override
    protected void configure() {
        // CloudTrail
        addCodec(CloudTrailCodec.NAME, CloudTrailCodec.class);
        addTransport(CloudTrailTransport.NAME, CloudTrailTransport.class);
        addMessageInput(CloudTrailInput.class);

        // CloudWatch
        addCodec(CloudWatchFlowLogCodec.NAME, CloudWatchFlowLogCodec.class);
        addCodec(CloudWatchRawLogCodec.NAME, CloudWatchRawLogCodec.class);
        addTransport(KinesisTransport.NAME, KinesisTransport.class);
        addMessageInput(FlowLogsInput.class);
        addMessageInput(CloudWatchLogsInput.class);

        // Instance name lookup
        addMessageProcessor(AWSInstanceNameLookupProcessor.class, AWSInstanceNameLookupProcessor.Descriptor.class);

        bind(InstanceLookupTable.class).asEagerSingleton();
        bind(ObjectMapper.class).annotatedWith(AWSObjectMapper.class).toInstance(createObjectMapper());
    }

    private ObjectMapper createObjectMapper() {
        return new ObjectMapper()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }
}
