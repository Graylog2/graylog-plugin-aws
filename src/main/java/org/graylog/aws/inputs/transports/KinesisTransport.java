package org.graylog.aws.inputs.transports;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.codahale.metrics.MetricSet;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.assistedinject.Assisted;
import okhttp3.HttpUrl;
import org.graylog.aws.AWSObjectMapper;
import org.graylog.aws.auth.AWSAuthProvider;
import org.graylog.aws.config.AWSPluginConfiguration;
import org.graylog.aws.kinesis.KinesisConsumer;
import org.graylog2.plugin.LocalMetricRegistry;
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
import org.graylog2.plugin.journal.RawMessage;
import org.graylog2.plugin.system.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class KinesisTransport extends ThrottleableTransport {
    private static final Logger LOG = LoggerFactory.getLogger(KinesisTransport.class);
    public static final String NAME = "awskinesis";

    private static final String CK_AWS_REGION = "aws_region";
    private static final String CK_ACCESS_KEY = "aws_access_key";
    private static final String CK_SECRET_KEY = "aws_secret_key";
    private static final String CK_ASSUME_ROLE_ARN = "aws_assume_role_arn";
    private static final String CK_KINESIS_STREAM_NAME = "kinesis_stream_name";
    private static final String CK_KINESIS_RECORD_BATCH_SIZE = "kinesis_record_batch_size";
    private static final String CK_KINESIS_MAX_THROTTLED_WAIT_MS = "kinesis_max_throttled_wait";

    private static final int DEFAULT_BATCH_SIZE = 10000;
    private static final int DEFAULT_THROTTLED_WAIT_MS = 60000;
    private static final int KINESIS_CONSUMER_STOP_WAIT_MS = 15000;

    private final Configuration configuration;
    private final org.graylog2.Configuration graylogConfiguration;
    private final NodeId nodeId;
    private final LocalMetricRegistry localRegistry;
    private final ClusterConfigService clusterConfigService;
    private final ObjectMapper objectMapper;

    private KinesisConsumer reader;
    private final ExecutorService executor;
    private Future<?> kinesisTaskFuture = null;

    /**
     * Indicates if the Kinesis consumer has been stopped due to throttling. Allows the consumer to be restarted
     * once throttling is cleared.
     */
    public AtomicBoolean stoppedDueToThrottling;
    public final AtomicReference<KinesisTransportState> consumerState;

    @Inject
    public KinesisTransport(@Assisted final Configuration configuration,
                            EventBus serverEventBus,
                            org.graylog2.Configuration graylogConfiguration,
                            final ClusterConfigService clusterConfigService,
                            final NodeId nodeId,
                            LocalMetricRegistry localRegistry,
                            @AWSObjectMapper ObjectMapper objectMapper) {
        super(serverEventBus, configuration);
        this.clusterConfigService = clusterConfigService;
        this.configuration = configuration;
        this.graylogConfiguration = graylogConfiguration;
        this.nodeId = nodeId;
        this.localRegistry = localRegistry;
        this.objectMapper = objectMapper;
        this.executor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
                                                                  .setDaemon(true)
                                                                  .setNameFormat("aws-kinesis-reader-%d")
                                                                  .setUncaughtExceptionHandler((t, e) -> LOG.error("Uncaught exception in AWS Kinesis reader.", e))
                                                                  .build());
        this.stoppedDueToThrottling = new AtomicBoolean(false);
        this.consumerState = new AtomicReference<>(KinesisTransportState.STOPPED);
    }

    @Override
    public void handleChangedThrottledState(boolean isThrottled) {

        if (!isThrottled) {
            LOG.info("Unthrottled");
        } else {
            LOG.info("Throttled");
        }

        if (!isThrottled && stoppedDueToThrottling.get()) {

            stoppedDueToThrottling.set(false);

            LOG.debug("Transport state [{}]", consumerState.get());

            switch (consumerState.get()) {
                case STOPPED:
                    LOG.info("[unthrottled] Throttle state ended restarting consumer");
                    restartConsumer();
                    break;
                case STOPPING: {

                    LOG.info("Kinesis consumer is still stopping. Waiting [{}ms] for the consumer to finish " +
                             "stopping before restarting.", KINESIS_CONSUMER_STOP_WAIT_MS);
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            if (consumerState.get() == KinesisTransportState.STOPPED) {
                                restartConsumer();
                            } else {
                                // TODO: Do we need to do something special in this case? Wait longer? Probably.
                                LOG.error("Could not restart Kinesis consumer, because the previously running " +
                                          "consumer did not reach a STOPPED state within [{}ms].", KINESIS_CONSUMER_STOP_WAIT_MS);
                            }
                        }
                    }, KINESIS_CONSUMER_STOP_WAIT_MS);
                }
            }
        }
    }

    private void restartConsumer() {
        kinesisTaskFuture = executor.submit(KinesisTransport.this.reader);
    }

    @Override
    public void doLaunch(MessageInput input) throws MisfireException {

        final AWSPluginConfiguration awsConfig = clusterConfigService.getOrDefault(AWSPluginConfiguration.class,
                                                                                   AWSPluginConfiguration.createDefault());
        AWSAuthProvider authProvider = new AWSAuthProvider(
                awsConfig, configuration.getString(CK_ACCESS_KEY),
                configuration.getString(CK_SECRET_KEY),
                configuration.getString(CK_AWS_REGION),
                configuration.getString(CK_ASSUME_ROLE_ARN)
        );

        this.reader = new KinesisConsumer(
                configuration.getString(CK_KINESIS_STREAM_NAME),
                Region.getRegion(Regions.fromName(configuration.getString(CK_AWS_REGION))),
                kinesisCallback(input),
                awsConfig,
                authProvider,
                nodeId,
                graylogConfiguration.getHttpProxyUri() == null ? null : HttpUrl.get(graylogConfiguration.getHttpProxyUri()),
                this,
                objectMapper,
                configuration.intIsSet(CK_KINESIS_MAX_THROTTLED_WAIT_MS) ? configuration.getInt(CK_KINESIS_MAX_THROTTLED_WAIT_MS) : null,
                configuration.getInt(CK_KINESIS_RECORD_BATCH_SIZE, DEFAULT_BATCH_SIZE)
        );

        LOG.info("Starting Kinesis reader thread for input [{}/{}]", input.getName(), input.getId());
        kinesisTaskFuture = executor.submit(this.reader);
    }

    private Consumer<byte[]> kinesisCallback(final MessageInput input) {
        return (data) -> input.processRawMessage(new RawMessage(data));
    }

    @Override
    public void doStop() {
        this.stoppedDueToThrottling.set(false); // Prevent restart of consumer when input is shutting down.
        if (this.reader != null) {
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
    public interface Factory extends Transport.Factory<KinesisTransport> {
        @Override
        KinesisTransport create(Configuration configuration);

        @Override
        Config getConfig();
    }

    @ConfigClass
    public static class Config extends ThrottleableTransport.Config {

        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            final ConfigurationRequest r = super.getRequestedConfiguration();

            r.addField(new NumberField(
                    CK_KINESIS_MAX_THROTTLED_WAIT_MS,
                    "Throttled wait milliseconds",
                    DEFAULT_THROTTLED_WAIT_MS,
                    "The maximum time that the Kinesis input will pause for when in a throttled state. If this time is exceeded, then the Kinesis consumer will shut down until the throttled state is cleared. Recommended default: 60,000 ms",
                    ConfigurationField.Optional.OPTIONAL,
                    NumberField.Attribute.ONLY_POSITIVE));

            r.addField(new DropdownField(
                    CK_AWS_REGION,
                    "AWS Region",
                    Regions.US_EAST_1.getName(),
                    buildRegionChoices(),
                    "The AWS region the Kinesis stream is running in.",
                    ConfigurationField.Optional.NOT_OPTIONAL
            ));

            r.addField(new TextField(
                    CK_ACCESS_KEY,
                    "AWS access key",
                    "",
                    "Access key of an AWS user with sufficient permissions. (See documentation)",
                    ConfigurationField.Optional.OPTIONAL
            ));

            r.addField(new TextField(
                    CK_SECRET_KEY,
                    "AWS secret key",
                    "",
                    "Secret key of an AWS user with sufficient permissions. (See documentation)",
                    ConfigurationField.Optional.OPTIONAL,
                    TextField.Attribute.IS_PASSWORD
            ));

            r.addField(new TextField(
                    CK_ASSUME_ROLE_ARN,
                    "AWS assume role ARN",
                    "",
                    "Role ARN with required permissions (cross account access)",
                    ConfigurationField.Optional.OPTIONAL
            ));

            r.addField(new TextField(
                    CK_KINESIS_STREAM_NAME,
                    "Kinesis Stream name",
                    "",
                    "The name of the Kinesis stream that receives your messages. See README for instructions on how to connect messages to a Kinesis Stream.",
                    ConfigurationField.Optional.NOT_OPTIONAL
            ));

            r.addField(new NumberField(
                    CK_KINESIS_RECORD_BATCH_SIZE,
                    "Kinesis Record batch size.",
                    DEFAULT_BATCH_SIZE,
                    "The number of Kinesis records to fetch at a time. Each record may be up to 1MB in size. The AWS default is 10,000. Enter a smaller value to process smaller chunks at a time.",
                    ConfigurationField.Optional.OPTIONAL,
                    NumberField.Attribute.ONLY_POSITIVE));

            return r;
        }
    }

    /**
     * Build a list of region choices with both a value (persisted in configuration) and display value (shown to the user).
     *
     * The display value is formatted nicely: "EU (London): eu-west-2"
     * The value is eventually passed to Regions.fromName() to get the actual region object: eu-west-2
     * @return a choices map with configuration value map keys and display value map values.
     */
    static Map<String, String> buildRegionChoices() {
        Map<String, String> regions = Maps.newHashMap();
        for (Regions region : Regions.values()) {

            String displayValue = String.format("%s: %s", region.getDescription(), region.getName());
            regions.put(region.getName(), displayValue);
        }
        return regions;
    }
}
