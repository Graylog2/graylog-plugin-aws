package org.graylog.aws.processors.instancelookup;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.graylog.aws.AWS;
import org.graylog.aws.config.AWSPluginConfiguration;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Messages;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.messageprocessors.MessageProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AWSInstanceNameLookupProcessor implements MessageProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(AWSInstanceNameLookupProcessor.class);

    // Field names that can contain IP addresses of AWS instances like from EC2 or ELB.
    private static final ImmutableList<String> TRANSLATABLE_FIELD_NAMES = ImmutableList.<String>builder()
            .add("src_addr")
            .add("dst_addr")
            .build();

    public static class Descriptor implements MessageProcessor.Descriptor {
        @Override
        public String name() {
            return "AWS Instance Name Lookup";
        }

        @Override
        public String className() {
            return AWSInstanceNameLookupProcessor.class.getCanonicalName();
        }
    }

    private final MetricRegistry metricRegistry;
    private final InstanceLookupTable table;

    private AWSPluginConfiguration config;

    @Inject
    public AWSInstanceNameLookupProcessor(ClusterConfigService clusterConfigService,
                                          MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
        this.table = InstanceLookupTable.getInstance();

        Runnable refresh = new Runnable() {
            @Override
            public void run() {
                try {
                    config = clusterConfigService.get(AWSPluginConfiguration.class);

                    if(config == null || !config.isComplete()) {
                        LOG.warn("AWS plugin is not fully configured. No instance lookups will happen.");
                        return;
                    }

                    if (!config.lookupsEnabled()) {
                        LOG.debug("AWS instance name lookups are disabled.");
                        return;
                    }

                    LOG.debug("Refreshing AWS instance lookup table.");

                    table.reload(config.getLookupRegions());
                } catch (Exception e) {
                    LOG.error("Could not refresh AWS instance lookup table.", e);
                }
            }
        };

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(
                new ThreadFactoryBuilder()
                        .setDaemon(true)
                        .setNameFormat("aws-instance-lookup-refresher-%d")
                        .setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                            @Override
                            public void uncaughtException(Thread t, Throwable e) {
                                LOG.error("Uncaught exception in AWS instance lookup refresher.", e);
                            }
                        })
                        .build()
        );

        executor.scheduleWithFixedDelay(refresh, 0, 60, TimeUnit.SECONDS);
    }

    @Override
    public Messages process(Messages messages) {
        if (config == null || !config.lookupsEnabled() || !table.isLoaded()) {
            return messages;
        }

        for (Message message : messages) {
            Object awsGroupId = message.getField(AWS.SOURCE_GROUP_IDENTIFIER);
            if(awsGroupId != null && awsGroupId.equals(true)) {
                // This is a message from one of our own inputs and we want to do a lookup.
                TRANSLATABLE_FIELD_NAMES.stream().filter(fieldName -> message.hasField(fieldName)).forEach(fieldName -> {
                    // Make it so!
                    message.addField(
                            fieldName + "_entity",
                            table.findByIp(message.getField(fieldName).toString()).getName()
                    );

                    message.addField(
                            fieldName + "_entity_description",
                            table.findByIp(message.getField(fieldName).toString()).getDescription()
                    );

                    message.addField(
                            fieldName + "_entity_aws_type",
                            table.findByIp(message.getField(fieldName).toString()).getAWSType()
                    );
                });

            }
        }

        return messages;
    }

}
