package org.graylog.aws.plugin;

import org.graylog.aws.inputs.cloudtrail.CloudTrailInput;
import org.graylog.aws.inputs.cloudtrail.CloudTrailCodec;
import org.graylog.aws.inputs.cloudtrail.CloudTrailTransport;
import org.graylog.aws.inputs.flowlogs.FlowLogCodec;
import org.graylog.aws.inputs.flowlogs.FlowLogTransport;
import org.graylog.aws.inputs.flowlogs.FlowLogsInput;
import org.graylog.aws.processors.instancelookup.AWSInstanceNameLookupProcessor;
import org.graylog2.plugin.PluginModule;

public class AWSModule extends PluginModule {
    @Override
    protected void configure() {
        // CloudTrail input
        addCodec(CloudTrailCodec.NAME, CloudTrailCodec.class);
        addTransport(CloudTrailTransport.NAME, CloudTrailTransport.class);
        addMessageInput(CloudTrailInput.class);

        // FlowLog input
        addCodec(FlowLogCodec.NAME, FlowLogCodec.class);
        addTransport(FlowLogTransport.NAME, FlowLogTransport.class);
        addMessageInput(FlowLogsInput.class);

        // Instance name lookup
        addMessageProcessor(AWSInstanceNameLookupProcessor.class, AWSInstanceNameLookupProcessor.Descriptor.class);
    }
}
