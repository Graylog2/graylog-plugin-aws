package org.graylog.aws.inputs.s3;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;

public class S3Input extends MessageInput {
    private static final String NAME = "AWS S3 Input";

    @AssistedInject
    public S3Input(@Assisted Configuration configuration,
                   MetricRegistry metricRegistry,
                   S3Transport.Factory transport,
                   LocalMetricRegistry localRegistry,
                   S3Codec.Factory codec,
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
                serverStatus);
    }

    @FactoryClass
    public interface Factory extends MessageInput.Factory<S3Input> {
        @Override
        S3Input create(Configuration configuration);

        @Override
        Config getConfig();

        @Override
        Descriptor getDescriptor();
    }

    public static class Descriptor extends MessageInput.Descriptor {
        @Inject
        public Descriptor() {
            super(NAME, false, "");
        }
    }

    @ConfigClass
    public static class Config extends MessageInput.Config {
        @Inject
        public Config(S3Transport.Factory transport, S3Codec.Factory codec) {
            super(transport.getConfig(), codec.getConfig());
        }
    }
}
