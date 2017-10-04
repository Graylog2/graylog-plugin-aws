package org.graylog.aws.inputs.codecs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.aws.cloudwatch.CloudWatchLogData;
import org.graylog.aws.cloudwatch.CloudWatchLogEvent;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.codecs.AbstractCodec;
import org.graylog2.plugin.inputs.codecs.CodecAggregator;
import org.graylog2.plugin.inputs.codecs.MultiMessageCodec;
import org.graylog2.plugin.journal.RawMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class CloudWatchLogDataCodec extends AbstractCodec implements MultiMessageCodec {
    private static final Logger LOG = LoggerFactory.getLogger(CloudWatchLogDataCodec.class);

    private final ObjectMapper objectMapper;

    CloudWatchLogDataCodec(Configuration configuration, ObjectMapper objectMapper) {
        super(configuration);
        this.objectMapper = objectMapper;
    }

    @Nullable
    @Override
    public Message decode(@Nonnull RawMessage rawMessage) {
        throw new UnsupportedOperationException("MultiMessageCodec " + getClass() + " does not support decode()");
    }

    @Nullable
    @Override
    public Collection<Message> decodeMessages(@Nonnull RawMessage rawMessage) {
        try {
            final CloudWatchLogData data = objectMapper.readValue(rawMessage.getPayload(), CloudWatchLogData.class);
            final List<Message> messages = new ArrayList<>(data.logEvents.size());

            for (final CloudWatchLogEvent logEvent : data.logEvents) {
                try {
                    final Message message = decodeLogData(logEvent);
                    if (message != null) {
                        messages.add(message);
                    }
                } catch (Exception e) {
                    LOG.error("Couldn't decode log event <{}>", logEvent);
                }
            }

            return messages;
        } catch (IOException e) {
            throw new RuntimeException("Couldn't deserialize log data", e);
        }
    }

    @Nullable
    protected abstract Message decodeLogData(@Nonnull final CloudWatchLogEvent event);

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
}
