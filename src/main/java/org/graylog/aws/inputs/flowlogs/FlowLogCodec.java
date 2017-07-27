package org.graylog.aws.inputs.flowlogs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.assistedinject.Assisted;
import org.graylog.aws.AWS;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.codecs.Codec;
import org.graylog2.plugin.inputs.codecs.CodecAggregator;
import org.graylog2.plugin.journal.RawMessage;
import org.joda.time.Seconds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

public class FlowLogCodec implements Codec {
    private static final Logger LOG = LoggerFactory.getLogger(FlowLogCodec.class);
    public static final String NAME = "AWSFlowLog";

    private final Configuration configuration;
    private final ObjectMapper objectMapper;

    private final IANAProtocolNumbers protocolNumbers;

    @Inject
    public FlowLogCodec(@Assisted Configuration configuration, ObjectMapper objectMapper) {
        this.configuration = configuration;
        this.objectMapper = objectMapper;

        this.protocolNumbers = new IANAProtocolNumbers();
    }

    @Nullable
    @Override
    public Message decode(@Nonnull RawMessage rawMessage) {
        try {
            String rawString = new String(rawMessage.getPayload());
            String[] parts = rawString.split(" ");

            if (parts.length != 15) {
                LOG.warn("Received FlowLog message with not exactly 15 fields. Skipping. Message was: [{}]", rawString);
                return null;
            }

            FlowLogMessage flowLogMessage = FlowLogMessage.fromParts(parts);

            Message result = new Message(
                    buildSummary(flowLogMessage),
                    "aws-flowlogs",
                    flowLogMessage.getTimestamp()
            );
            result.addFields(buildFields(flowLogMessage));
            result.addField(AWS.SOURCE_GROUP_IDENTIFIER, true);

            return result;
        } catch (Exception e) {
            throw new RuntimeException("Could not deserialize AWS FlowLog record.", e);
        }
    }

    private String buildSummary(FlowLogMessage msg) {
        return new StringBuilder()
                .append(msg.getInterfaceId()).append(" ")
                .append(msg.getAction()).append(" ")
                .append(protocolNumbers.lookup(msg.getProtocolNumber())).append(" ")
                .append(msg.getSourceAddress()).append(":").append(msg.getSourcePort())
                .append(" -> ")
                .append(msg.getDestinationAddress()).append(":").append(msg.getDestinationPort())
                .toString();
    }

    private Map<String, Object> buildFields(FlowLogMessage msg) {
        return new HashMap<String, Object>(){{
            put("account_id", msg.getAccountId());
            put("interface_id", msg.getInterfaceId());
            put("src_addr", msg.getSourceAddress());
            put("dst_addr", msg.getDestinationAddress());
            put("src_port", msg.getSourcePort());
            put("dst_port", msg.getDestinationPort());
            put("protocol_number", msg.getProtocolNumber());
            put("protocol", protocolNumbers.lookup(msg.getProtocolNumber()));
            put("packets", msg.getPackets());
            put("bytes", msg.getBytes());
            put("capture_window_duration_seconds", Seconds.secondsBetween(msg.getCaptureWindowStart(), msg.getCaptureWindowEnd()).getSeconds());
            put("action", msg.getAction());
            put("log_status", msg.getLogStatus());
        }};
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
    public interface Factory extends Codec.Factory<FlowLogCodec> {
        @Override
        FlowLogCodec create(Configuration configuration);

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
