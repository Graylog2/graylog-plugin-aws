package org.graylog.aws.inputs.codecs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.aws.cloudwatch.CloudWatchLogEntry;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.codecs.AbstractCodec;
import org.graylog2.plugin.inputs.codecs.CodecAggregator;
import org.graylog2.plugin.journal.RawMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;

public abstract class CloudWatchLogDataCodec extends AbstractCodec {
    private static final Logger LOG = LoggerFactory.getLogger(CloudWatchLogDataCodec.class);

    private final ObjectMapper objectMapper;

    CloudWatchLogDataCodec(Configuration configuration, ObjectMapper objectMapper) {
        super(configuration);
        this.objectMapper = objectMapper;
    }

    @Nullable
    @Override
    public Message decode(@Nonnull RawMessage rawMessage) {
        try {
            final CloudWatchLogEntry entry = objectMapper.readValue(rawMessage.getPayload(), CloudWatchLogEntry.class);

            try {
                return decodeLogData(entry, entry.logGroup, entry.logStream);
            } catch (Exception e) {
                LOG.error("Couldn't decode log event <{}>", entry);

                // TODO: What is the effect of returning null here?
                return null;
            }
        } catch (IOException e) {
            throw new RuntimeException("Couldn't deserialize log data", e);
        }
    }

    @Nullable
    protected abstract Message decodeLogData(@Nonnull final CloudWatchLogEntry event, @Nonnull final String logGroup, @Nonnull final String logStream);

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
