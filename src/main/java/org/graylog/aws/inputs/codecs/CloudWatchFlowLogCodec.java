package org.graylog.aws.inputs.codecs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.assistedinject.Assisted;
import org.graylog.aws.AWS;
import org.graylog.aws.AWSObjectMapper;
import org.graylog.aws.cloudwatch.CloudWatchLogEntry;
import org.graylog.aws.cloudwatch.FlowLogMessage;
import org.graylog.aws.inputs.cloudtrail.CloudTrailCodec;
import org.graylog.aws.inputs.flowlogs.IANAProtocolNumbers;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.codecs.AbstractCodec;
import org.graylog2.plugin.inputs.codecs.Codec;
import org.joda.time.Seconds;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

public class CloudWatchFlowLogCodec extends CloudWatchLogDataCodec {
    public static final String NAME = "AWSFlowLog";

    private final IANAProtocolNumbers protocolNumbers;

    @Inject
    public CloudWatchFlowLogCodec(@Assisted Configuration configuration, @AWSObjectMapper ObjectMapper objectMapper) {
        super(configuration, objectMapper);
        this.protocolNumbers = new IANAProtocolNumbers();
    }

    @Nullable
    @Override
    public Message decodeLogData(@Nonnull final CloudWatchLogEntry logEvent, @Nonnull final String logGroup, @Nonnull final String logStream) {
        try {
            final FlowLogMessage flowLogMessage = FlowLogMessage.fromLogEvent(logEvent);

            if (flowLogMessage == null) {
                return null;
            }

            final String source = configuration.getString(CloudTrailCodec.Config.CK_OVERRIDE_SOURCE, "aws-flowlogs");
            final Message result = new Message(
                    buildSummary(flowLogMessage),
                    source,
                    flowLogMessage.getTimestamp()
            );
            result.addFields(buildFields(flowLogMessage));
            result.addField(AWS.FIELD_LOG_GROUP, logGroup);
            result.addField(AWS.FIELD_LOG_STREAM, logStream);
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
        return new HashMap<String, Object>() {{
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

    @Override
    public String getName() {
        return NAME;
    }

    @FactoryClass
    public interface Factory extends Codec.Factory<CloudWatchFlowLogCodec> {
        @Override
        CloudWatchFlowLogCodec create(Configuration configuration);

        @Override
        Config getConfig();
    }

    @ConfigClass
    public static class Config extends AbstractCodec.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            return new ConfigurationRequest();
        }

        @Override
        public void overrideDefaultValues(@Nonnull ConfigurationRequest cr) {
        }
    }
}
