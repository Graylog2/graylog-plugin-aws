package org.graylog.aws.inputs.cloudtrail;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.codahale.metrics.MetricSet;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import okhttp3.HttpUrl;
import org.graylog.aws.config.AWSPluginConfiguration;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.BooleanField;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.DropdownField;
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

import javax.annotation.Nullable;
import javax.inject.Named;
import java.net.URI;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class CloudTrailTransport extends ThrottleableTransport {
    private static final Logger LOG = LoggerFactory.getLogger(CloudTrailTransport.class);
    public static final String NAME = "cloudtrail";

    private static final String CK_LEGACY_AWS_REGION = "aws_region";
    private static final String CK_AWS_SQS_REGION = "aws_sqs_region";
    private static final String CK_AWS_S3_REGION = "aws_s3_region";
    private static final String CK_SQS_NAME = "aws_sqs_queue_name";
    private static final String CK_USE_PROXY = "use_proxy";

    private static final Regions DEFAULT_REGION = Regions.US_EAST_1;

    private final ServerStatus serverStatus;
    private final URI httpProxyUri;
    private final LocalMetricRegistry localRegistry;
    private final ClusterConfigService clusterConfigService;

    private CloudTrailSubscriber subscriber;

    @AssistedInject
    public CloudTrailTransport(@Assisted final Configuration configuration,
                               final ClusterConfigService clusterConfigService,
                               final EventBus serverEventBus,
                               final ServerStatus serverStatus,
                               @Named("http_proxy_uri") @Nullable URI httpProxyUri,
                               LocalMetricRegistry localRegistry) {
        super(serverEventBus, configuration);

        this.clusterConfigService = clusterConfigService;
        this.serverStatus = serverStatus;
        this.httpProxyUri = httpProxyUri;
        this.localRegistry = localRegistry;
    }

    @Override
    public void setMessageAggregator(CodecAggregator aggregator) {
        // Not supported.
    }

    @Subscribe
    public void lifecycleStateChange(Lifecycle lifecycle) {
        LOG.debug("Lifecycle changed to {}", lifecycle);
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

        AWSPluginConfiguration config = clusterConfigService.get(AWSPluginConfiguration.class);

        LOG.info("Starting cloud trail subscriber");

        final String legacyRegionName = input.getConfiguration().getString(CK_LEGACY_AWS_REGION, DEFAULT_REGION.getName());
        final String sqsRegionName = input.getConfiguration().getString(CK_AWS_SQS_REGION, legacyRegionName);
        final String s3RegionName = input.getConfiguration().getString(CK_AWS_S3_REGION, legacyRegionName);

        final HttpUrl proxyUrl = input.getConfiguration().getBoolean(CK_USE_PROXY, false) ? HttpUrl.get(httpProxyUri) : null;

        subscriber = new CloudTrailSubscriber(
                Region.getRegion(Regions.fromName(sqsRegionName)),
                Region.getRegion(Regions.fromName(s3RegionName)),
                input.getConfiguration().getString(CK_SQS_NAME),
                input,
                config.accessKey(),
                config.secretKey(),
                proxyUrl
        );

        subscriber.start();
    }

    @Override
    public void doStop() {
        LOG.info("Stopping cloud trail subscriber");
        if (subscriber != null) {
            subscriber.terminate();
        }
    }

    @Override
    public MetricSet getMetricSet() {
        return localRegistry;
    }

    @FactoryClass
    public interface Factory extends Transport.Factory<CloudTrailTransport> {
        @Override
        CloudTrailTransport create(Configuration configuration);

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
                    "The AWS region the S3 bucket containing CloudTrail logs is in.",
                    ConfigurationField.Optional.NOT_OPTIONAL
            ));

            r.addField(new TextField(
                    CK_SQS_NAME,
                    "SQS queue name",
                    "cloudtrail-notifications",
                    "The SQS queue that SNS is writing CloudTrail notifications to.",
                    ConfigurationField.Optional.NOT_OPTIONAL
            ));

            r.addField(new BooleanField(
                    CK_USE_PROXY,
                    "Use proxy server",
                    false,
                    "Use proxy server (see 'http_proxy_uri' in the Graylog configuration) for accessing the AWS API."
            ));

            return r;
        }
    }
}
