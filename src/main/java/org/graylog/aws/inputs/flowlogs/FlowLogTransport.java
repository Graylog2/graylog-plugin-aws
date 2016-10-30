package org.graylog.aws.inputs.flowlogs;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.codahale.metrics.MetricSet;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
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

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class FlowLogTransport implements Transport {
    private static final Logger LOG = LoggerFactory.getLogger(FlowLogTransport.class);
    public static final String NAME = "flowlog";

    private static final String CK_AWS_REGION = "aws_region";
    private static final String CK_KINESIS_STREAM_NAME = "kinesis_stream_name";

    private final Configuration configuration;
    private final LocalMetricRegistry localRegistry;
    private final ClusterConfigService clusterConfigService;

    private FlowLogReader reader;

    @AssistedInject
    public FlowLogTransport(@Assisted final Configuration configuration,
                               final ClusterConfigService clusterConfigService,
                               LocalMetricRegistry localRegistry) {
        this.clusterConfigService = clusterConfigService;
        this.configuration = configuration;
        this.localRegistry = localRegistry;
    }

    @Override
    public void launch(MessageInput input) throws MisfireException {
        ExecutorService executor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat("aws-flowlog-reader-%d")
                .setUncaughtExceptionHandler((t, e) -> LOG.error("Uncaught exception in AWS FlowLogs reader.", e))
                .build());

        this.reader = new FlowLogReader(
                this.configuration.getString(CK_KINESIS_STREAM_NAME),
                Region.getRegion(Regions.fromName(this.configuration.getString(CK_AWS_REGION))),
                input,
                clusterConfigService
        );

        LOG.info("Starting FlowLogs Kinesis reader thread.");

        executor.submit(this.reader);
    }

    @Override
    public void stop() {
        if(this.reader != null) {
            this.reader.stop();
        }
    }

    @Override
    public void setMessageAggregator(CodecAggregator aggregator) {
        // Not supported.
    }

    @Override
    public MetricSet getMetricSet() {
        return localRegistry;
    }

    @FactoryClass
    public interface Factory extends Transport.Factory<FlowLogTransport> {
        @Override
        FlowLogTransport create(Configuration configuration);

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
                    "The AWS region the FlowLogs are stored in.",
                    ConfigurationField.Optional.NOT_OPTIONAL
            ));

            r.addField(new TextField(
                    CK_KINESIS_STREAM_NAME,
                    "Kinesis Stream name",
                    "",
                    "The name of the Kinesis Stream that receives your FlowLog messages. See README for instructions on how to connect FlowLogs to a Kinesis Stream.",
                    ConfigurationField.Optional.NOT_OPTIONAL
            ));

            return r;
        }
    }

}
