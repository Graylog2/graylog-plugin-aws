package org.graylog.aws.plugin;

import org.graylog.aws.inputs.cloudtrail.CloudTrailInput;
import org.graylog.aws.inputs.cloudtrail.CloudTrailCodec;
import org.graylog.aws.inputs.cloudtrail.CloudTrailTransport;
import org.graylog2.plugin.PluginModule;

public class AWSInputModule extends PluginModule {
    @Override
    protected void configure() {
        // CloudTrail input
        addCodec(CloudTrailCodec.NAME, CloudTrailCodec.class);
        addTransport(CloudTrailTransport.NAME, CloudTrailTransport.class);
        addMessageInput(CloudTrailInput.class);


    }
}
