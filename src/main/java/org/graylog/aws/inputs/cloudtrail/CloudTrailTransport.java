package org.graylog.aws.inputs.cloudtrail;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.codahale.metrics.MetricSet;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog.aws.config.AWSPluginConfiguration;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
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

import java.util.Map;

public class CloudTrailTransport extends ThrottleableTransport {
    private static final Logger LOG = LoggerFactory.getLogger(CloudTrailTransport.class);
    public static final String NAME = "cloudtrail";

    private static final String CK_AWS_REGION = "aws_region";
    private static final String CK_SQS_NAME = "aws_sqs_queue_name";

    private final ServerStatus serverStatus;
    private final LocalMetricRegistry localRegistry;
    private final ClusterConfigService clusterConfigService;

    private CloudTrailSubscriber subscriber;

    @AssistedInject
    public CloudTrailTransport(@Assisted final Configuration configuration,
                               final ClusterConfigService clusterConfigService,
                               final EventBus serverEventBus,
                               final ServerStatus serverStatus,
                               LocalMetricRegistry localRegistry) {
        super(serverEventBus, configuration);

        this.clusterConfigService = clusterConfigService;
        this.serverStatus = serverStatus;
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
        serverStatus.awaitRunning(new Runnable() {
            @Override
            public void run() {
                lifecycleStateChange(Lifecycle.RUNNING);
            }
        });

        AWSPluginConfiguration config = clusterConfigService.get(AWSPluginConfiguration.class);

        LOG.info("Starting cloud trail subscriber");

        subscriber = new CloudTrailSubscriber(
                Region.getRegion(Regions.fromName(input.getConfiguration().getString(CK_AWS_REGION))),
                input.getConfiguration().getString(CK_SQS_NAME),
                input,
                config.accessKey(),
                config.secretKey()
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

            Map<String, String> regions = Maps.newHashMap();
            for (Regions region : Regions.values()) {
                regions.put(region.getName(), region.toString());
            }

            r.addField(new DropdownField(
                    CK_AWS_REGION,
                    "AWS Region",
                    Regions.US_EAST_1.getName(),
                    regions,
                    "The AWS region to read CloudTrail for. The configured SQS queue " +
                            "must also be located in this region.",
                    ConfigurationField.Optional.NOT_OPTIONAL
            ));

            r.addField(new TextField(
                    CK_SQS_NAME,
                    "SQS queue name",
                    "cloudtrail-notifications",
                    "The SQS queue that SNS is writing CloudTrail notifications to.",
                    ConfigurationField.Optional.NOT_OPTIONAL
            ));

            return r;
        }
    }
}
