package org.graylog.aws.inputs.codecs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.aws.cloudwatch.CloudWatchLogData;
import org.graylog.aws.cloudwatch.CloudWatchLogEvent;
import org.graylog2.plugin.Message;
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

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

public abstract class CloudWatchLogDataCodec implements MultiMessageCodec {
    private static final Logger LOG = LoggerFactory.getLogger(CloudWatchLogDataCodec.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().configure(FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Nullable
    @Override
    public Message decode(@Nonnull RawMessage rawMessage) {
        throw new UnsupportedOperationException("MultiMessageCodec " + getClass() + " does not support decode()");
    }

    @Nullable
    @Override
    public Collection<Message> decodeMessages(@Nonnull RawMessage rawMessage) {
        try {
            final CloudWatchLogData data = OBJECT_MAPPER.readValue(rawMessage.getPayload(), CloudWatchLogData.class);
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
}
