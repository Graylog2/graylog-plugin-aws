package org.graylog.aws.inputs.codecs;

import com.google.inject.assistedinject.Assisted;
import org.graylog.aws.cloudwatch.CloudWatchLogEvent;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.codecs.Codec;
import org.graylog2.plugin.inputs.codecs.CodecAggregator;
import org.joda.time.DateTime;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;

public class CloudWatchRawLogCodec extends CloudWatchLogDataCodec {
    public static final String NAME = "AWSCloudWatchRawLog";

    private final Configuration configuration;

    @Inject
    public CloudWatchRawLogCodec(@Assisted Configuration configuration) {
        this.configuration = configuration;
    }

    @Nullable
    @Override
    public Message decodeLogData(@Nonnull final CloudWatchLogEvent logEvent) {
        try {
            return new Message(logEvent.message, "aws-raw-logs", new DateTime(logEvent.timestamp));
        } catch (Exception e) {
            throw new RuntimeException("Could not deserialize AWS FlowLog record.", e);
        }
    }

    @Nonnull
    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

    @Nullable
    @Override
    public CodecAggregator getAggregator() {
        return null;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @FactoryClass
    public interface Factory extends Codec.Factory<CloudWatchRawLogCodec> {
        @Override
        CloudWatchRawLogCodec create(Configuration configuration);

        @Override
        Config getConfig();
    }

    @ConfigClass
    public static class Config implements Codec.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            return new ConfigurationRequest();
        }

        @Override
        public void overrideDefaultValues(@Nonnull ConfigurationRequest cr) {
        }
    }
}
