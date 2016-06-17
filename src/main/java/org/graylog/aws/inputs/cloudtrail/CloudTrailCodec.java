package org.graylog.aws.inputs.cloudtrail;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog.aws.inputs.cloudtrail.json.CloudTrailRecord;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.codecs.Codec;
import org.graylog2.plugin.inputs.codecs.CodecAggregator;
import org.graylog2.plugin.journal.RawMessage;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CloudTrailCodec implements Codec {
    private static final Logger LOG = LoggerFactory.getLogger(CloudTrailCodec.class);
    public static final String NAME = "AWSCloudTrail";

    private final Configuration configuration;
    private final ObjectMapper objectMapper;

    @AssistedInject
    public CloudTrailCodec(@Assisted Configuration configuration, ObjectMapper objectMapper) {
        this.configuration = configuration;
        this.objectMapper = objectMapper;
    }

    @Nullable
    @Override
    public Message decode(@Nonnull RawMessage rawMessage) {
        try {
            final CloudTrailRecord record = objectMapper.readValue(rawMessage.getPayload(), CloudTrailRecord.class);
            final Message message = new Message(record.getConstructedMessage(), "aws-cloudtrail", DateTime.parse(record.eventTime));

            message.addFields(record.additionalFieldsAsMap());
            message.addField("full_message", record.getFullMessage());

            return message;
        } catch (Exception e) {
            throw new RuntimeException("Could not deserialize CloudTrail record.", e);
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
    public interface Factory extends Codec.Factory<CloudTrailCodec> {
        @Override
        CloudTrailCodec create(Configuration configuration);

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
