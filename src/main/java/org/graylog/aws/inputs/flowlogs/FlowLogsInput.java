package org.graylog.aws.inputs.flowlogs;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.assistedinject.Assisted;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;

import javax.inject.Inject;

public class FlowLogsInput extends MessageInput {
    private static final String NAME = "AWS FlowLogs Input";

    @Inject
    public FlowLogsInput(@Assisted Configuration configuration,
                           MetricRegistry metricRegistry,
                           FlowLogTransport.Factory transport,
                           LocalMetricRegistry localRegistry,
                           FlowLogCodec.Factory codec,
                           Config config,
                           Descriptor descriptor,
                           ServerStatus serverStatus) {
        super(
                metricRegistry,
                configuration,
                transport.create(configuration),
                localRegistry,
                codec.create(configuration),
                config,
                descriptor,
                serverStatus
        );
    }

    @FactoryClass
    public interface Factory extends MessageInput.Factory<FlowLogsInput> {
        @Override
        FlowLogsInput create(Configuration configuration);

        @Override
        Config getConfig();

        @Override
        Descriptor getDescriptor();
    }

    public static class Descriptor extends MessageInput.Descriptor {
        public Descriptor() {
            super(NAME, false, "");
        }
    }

    @ConfigClass
    public static class Config extends MessageInput.Config {
        @Inject
        public Config(FlowLogTransport.Factory transport, FlowLogCodec.Factory codec) {
            super(transport.getConfig(), codec.getConfig());
        }
    }

}
