package org.graylog.aws.inputs.codecs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.assistedinject.Assisted;
import org.graylog.aws.cloudwatch.CloudWatchLogEvent;
import org.graylog.aws.inputs.cloudtrail.CloudTrailCodec;
import org.graylog.aws.plugin.AWSObjectMapper;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.codecs.AbstractCodec;
import org.graylog2.plugin.inputs.codecs.Codec;
import org.joda.time.DateTime;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;

public class CloudWatchRawLogCodec extends CloudWatchLogDataCodec {
    public static final String NAME = "AWSCloudWatchRawLog";

    @Inject
    public CloudWatchRawLogCodec(@Assisted Configuration configuration, @AWSObjectMapper ObjectMapper objectMapper) {
        super(configuration, objectMapper);
    }

    @Nullable
    @Override
    public Message decodeLogData(@Nonnull final CloudWatchLogEvent logEvent, @Nonnull final String logGroup, @Nonnull final String logStream) {
        try {
            final String source = configuration.getString(CloudTrailCodec.Config.CK_OVERRIDE_SOURCE, "aws-raw-logs");
            Message result = new Message(
                    logEvent.message,
                    source,
                    new DateTime(logEvent.timestamp)
            );
            result.addField("aws_log_group", logGroup);
            result.addField("aws_log_stream", logStream);

            return result;
        } catch (Exception e) {
            throw new RuntimeException("Could not deserialize AWS FlowLog record.", e);
        }
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
    public static class Config extends AbstractCodec.Config {
    }
}
