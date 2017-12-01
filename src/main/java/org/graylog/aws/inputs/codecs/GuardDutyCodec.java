package org.graylog.aws.inputs.codecs;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.inject.assistedinject.Assisted;
import org.graylog.aws.inputs.guardduty.json.GuardDutyFinding;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.codecs.AbstractCodec;
import org.graylog2.plugin.inputs.codecs.Codec;
import org.graylog2.plugin.journal.RawMessage;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Map;

public class GuardDutyCodec extends AbstractCodec {

    private static final Logger LOG = LoggerFactory.getLogger(GuardDutyCodec.class);

    public static final String NAME = "AWSGuardDuty";

    private final ObjectMapper objectMapper;

    @Inject
    public GuardDutyCodec(@Assisted Configuration configuration, ObjectMapper objectMapper) {
        super(configuration);
        this.objectMapper = objectMapper;
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
    }

    @Nullable
    @Override
    public Message decode(@Nonnull RawMessage rawMessage) {
        try {
            // XXX TODO: set to DEBUG before pushering
            LOG.info("Raw GuardDuty finding JSON: {}", new String(rawMessage.getPayload()));

            GuardDutyFinding finding = objectMapper.readValue(rawMessage.getPayload(), GuardDutyFinding.class);

            // Shield about people messing up the routing and sending wrong events into the connected Kinesis stream.
            if(!"aws.guardduty".equals(finding.source)) {
                LOG.warn("AWS GuardDuty input received a non-GuardDuty message of type [{}].", finding.source);
                return null;
            }

            Message message = new Message(
                    "GuardDuty finding: " + finding.detail.title,
                    "aws-guardduty",
                    new DateTime(finding.detail.createdAt));

            message.addFields(buildFields(finding));

            return message;
        } catch(Exception e) {
            LOG.error("Could not decode GuardDuty finding.", e);
            return null;
        }
    }

    private Map<String, Object> buildFields(GuardDutyFinding finding) {
        // ImmutableMap does not allow NULL values and some values might be NULL. Using a HashMap for simplicity.
        Map<String, Object> fields = Maps.newHashMap();
        fields.put("account_id", finding.account);
        fields.put("region", finding.detail.region);
        fields.put("severity", finding.detail.severity);
        fields.put("description", finding.detail.description);

        return fields;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @FactoryClass
    public interface Factory extends Codec.Factory<GuardDutyCodec> {
        @Override
        GuardDutyCodec create(Configuration configuration);

        @Override
        GuardDutyCodec.Config getConfig();
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
