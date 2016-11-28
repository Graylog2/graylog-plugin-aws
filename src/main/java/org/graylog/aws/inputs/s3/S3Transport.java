package org.graylog.aws.inputs.s3;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.codahale.metrics.MetricSet;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.DropdownField;
import org.graylog2.plugin.configuration.fields.NumberField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.MisfireException;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.codecs.CodecAggregator;
import org.graylog2.plugin.inputs.transports.ThrottleableTransport;
import org.graylog2.plugin.inputs.transports.Transport;
import org.graylog2.plugin.lifecycles.Lifecycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class S3Transport extends ThrottleableTransport {
    private static final Logger LOG = LoggerFactory.getLogger(S3Transport.class);
    public static final String NAME = "AWS S3 Input";

    private static final String CK_LEGACY_AWS_REGION = "aws_region";
    private static final String CK_AWS_SQS_REGION = "aws_sqs_region";
    private static final String CK_AWS_S3_REGION = "aws_s3_region";
    private static final String CK_SQS_NAME = "aws_sqs_queue_name";
    private static final String CK_ACCESS_KEY = "aws_access_key";
    private static final String CK_SECRET_KEY = "aws_secret_key";
    private static final String CK_THREAD_COUNT = "aws_thread_count";

    private static final Regions DEFAULT_REGION = Regions.US_EAST_1;

    private final ServerStatus serverStatus;
    private final LocalMetricRegistry localRegistry;

    private S3Subscriber subscriber;

    @AssistedInject
    public S3Transport(@Assisted final Configuration configuration,
                       final EventBus serverEventBus,
                       final ServerStatus serverStatus,
                       LocalMetricRegistry localRegistry) {
        super(serverEventBus, configuration);
        this.serverStatus = serverStatus;
        this.localRegistry = localRegistry;
    }

    @Override
    public void setMessageAggregator(CodecAggregator aggregator) {
        // Not supported.
    }

    @Subscribe
    public void lifecycleStateChange(Lifecycle lifecycle) {
        LOG.info("Lifecycle changed to {}", lifecycle);
        switch (lifecycle) {
            case PAUSED:
            case FAILED:
            case HALTING:
                if (subscriber != null) {
                    subscriber.pause();
                }
                break;
            default:
                if (subscriber != null) {
                    subscriber.unpause();
                }
                break;
        }
    }

    @Override
    public void doLaunch(MessageInput input) throws MisfireException {
        serverStatus.awaitRunning(() -> lifecycleStateChange(Lifecycle.RUNNING));

        LOG.info("Starting s3 subscriber");

        final String legacyRegionName = input.getConfiguration().getString(CK_LEGACY_AWS_REGION, DEFAULT_REGION.getName());
        final String sqsRegionName = input.getConfiguration().getString(CK_AWS_SQS_REGION, legacyRegionName);
        final String s3RegionName = input.getConfiguration().getString(CK_AWS_S3_REGION, legacyRegionName);

        subscriber = new S3Subscriber(
                Region.getRegion(Regions.fromName(sqsRegionName)),
                Region.getRegion(Regions.fromName(s3RegionName)),
                input.getConfiguration().getString(CK_SQS_NAME),
                input,
                input.getConfiguration().getString(CK_ACCESS_KEY),
                input.getConfiguration().getString(CK_SECRET_KEY),
                input.getConfiguration().getInt(CK_THREAD_COUNT)
        );

        subscriber.start();
    }

    @Override
    public void doStop() {
        LOG.info("Stopping s3 subscriber");
        if (subscriber != null) {
            subscriber.terminate();
        }
    }

    @Override
    public MetricSet getMetricSet() {
        return localRegistry;
    }

    @FactoryClass
    public interface Factory extends Transport.Factory<S3Transport> {
        @Override
        S3Transport create(Configuration configuration);

        @Override
        Config getConfig();
    }

    @ConfigClass
    public static class Config extends ThrottleableTransport.Config {

        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            final ConfigurationRequest r = super.getRequestedConfiguration();

            final Map<String, String> regions = Arrays.stream(Regions.values())
                    .collect(Collectors.toMap(Regions::getName, Regions::toString));

            r.addField(new DropdownField(
                    CK_AWS_SQS_REGION,
                    "AWS SQS Region",
                    DEFAULT_REGION.getName(),
                    regions,
                    "The AWS region the SQS queue is in.",
                    ConfigurationField.Optional.NOT_OPTIONAL
            ));

            r.addField(new DropdownField(
                    CK_AWS_S3_REGION,
                    "AWS S3 Region",
                    DEFAULT_REGION.getName(),
                    regions,
                    "The AWS region the S3 bucket containing logs is in.",
                    ConfigurationField.Optional.NOT_OPTIONAL
            ));

            r.addField(new TextField(
                    CK_SQS_NAME,
                    "SQS queue name",
                    "s3-notifications",
                    "The SQS queue that SNS is writing S3 notifications to.",
                    ConfigurationField.Optional.NOT_OPTIONAL
            ));

            r.addField(new TextField(
                    CK_ACCESS_KEY,
                    "AWS access key",
                    "",
                    "Access key of an AWS user with sufficient permissions. Leave blank to use Instance Profile Credentials. (See documentation)",
                    ConfigurationField.Optional.OPTIONAL,
                    TextField.Attribute.IS_PASSWORD
            ));

            r.addField(new TextField(
                    CK_SECRET_KEY,
                    "AWS secret key",
                    "",
                    "Secret key of an AWS user with sufficient permissions. Leave blank to use Instance Profile Credentials. (See documentation)",
                    ConfigurationField.Optional.OPTIONAL,
                    TextField.Attribute.IS_PASSWORD
            ));

            r.addField(new NumberField(
                    CK_THREAD_COUNT,
                    "Processing thread count",
                    10,
                    "Number of processing threads to pool for SQS/S3 reading",
                    ConfigurationField.Optional.NOT_OPTIONAL,
                    NumberField.Attribute.ONLY_POSITIVE
            ));

            return r;
        }
    }
}