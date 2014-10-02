package com.graylog2.input;

import com.amazonaws.regions.Regions;
import com.google.common.collect.Maps;
import com.graylog2.input.cloudtrail.messages.TreeReader;
import com.graylog2.input.s3.S3Reader;
import com.graylog2.input.cloudtrail.notifications.CloudtrailSNSNotification;
import com.graylog2.input.cloudtrail.notifications.CloudtrailSQSSubscriber;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.buffers.Buffer;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.DropdownField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.MisfireException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class AWSInput extends MessageInput {

    private static final Logger LOG = LoggerFactory.getLogger(AWSInput.class);

    /*
     * TODO:
     *   * set up SNS/SQS bindings automatically (?)
     *   * read credentials from config
     *   * allow to configure region
     *   * allow to configure queue name
     *   * delete messages that were successfully read
     *   * make sleep interval configurable
     *   * make this thing stoppable
     */

    public static final int SLEEP_INTERVAL_SECS = 5;

    private static final String NAME = "AWS Input";

    private static final String CK_AWS_REGION = "aws_region";
    private static final String CK_SQS_NAME = "aws_sqs_queue_name";
    private static final String CK_ACCESS_KEY = "aws_access_key";
    private static final String CK_SECRET_KEY = "aws_secret_key";

    private boolean stopped = false;

    @Override
    public void checkConfiguration(Configuration configuration) throws ConfigurationException {
        if(!stringIsSet(CK_AWS_REGION) || !stringIsSet(CK_SQS_NAME)
                || !stringIsSet(CK_ACCESS_KEY) || !stringIsSet(CK_SECRET_KEY)) {
            throw new ConfigurationException("Not all required configuration fields are set.");
        }
    }

    @Override
    public void launch(Buffer buffer) throws MisfireException {
        CloudtrailSQSSubscriber subscriber = new CloudtrailSQSSubscriber("cloudtrail-write");

        TreeReader reader = new TreeReader();

        while(!stopped) {
            while(true) {
                List<CloudtrailSNSNotification> notifications = subscriber.getNotifications();

                /*
                 * Break out and wait a few seconds until next attempt to avoid hammering AWS with SQS
                 * read requests while still being able to read lots of queued notifications without
                 * the sleep() between each.
                 */
                if (notifications.size() == 0) {
                    LOG.debug("No more messages to read from SQS. Going into sleep loop.");
                    break;
                }

                for (CloudtrailSNSNotification n : notifications) {
                    try {
                        LOG.debug("Checking for CloudTrail notifications in SQS.");

                        List<Message> messages = reader.read(
                                S3Reader.readCompressed(
                                        n.getS3Bucket(),
                                        n.getS3ObjectKey()
                                )
                        );

                        for (Message message : messages) {
                            /*
                             * We are using insertCached and not insertFailFast here even though we are using a
                             * queue system (SQS) that could just deliver the message again when we are out of
                             * internal Graylog2 capacity.
                             *
                             * Reason is that every notification in SQS contains batches of CloudTrail messages
                             * that must be handled separately by Graylog2 (this loop) and we can only acknowledge
                             * the SQS notification that may include multiple CloudTrail messages. If one single
                             * internal message write fails, we would have to leave the whole notification on the
                             * queue and then possibly duplicate messages that did not fail later in subsequent
                             * write attempts.
                             *
                             * lol computers.
                             */
                            buffer.insertCached(message, this);
                        }

                        // All messages written. Ack notification.
                        subscriber.deleteNotification(n);
                    } catch (Exception e) {
                        // TODO: what if the file just doesn't exist? separate between things that can be just skipped forever and stuff that needs to be retried.
                        LOG.error("Could not readCompressed CloudTrail log file for <{}>. Skipping.", n.getS3Bucket(), e);
                        continue;
                    }
                }
            }

            try {
                LOG.debug("Waiting {} seconds until next CloudTrail SQS check.", SLEEP_INTERVAL_SECS);
                Thread.sleep(SLEEP_INTERVAL_SECS * 1000);
            } catch (InterruptedException ignored) {}
        }
    }

    @Override
    public void stop() {
        this.stopped = true;
    }

    @Override
    public ConfigurationRequest getRequestedConfiguration() {
        ConfigurationRequest r  = new ConfigurationRequest();

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

        r.addField(new TextField(
                CK_ACCESS_KEY,
                "AWS access key",
                "",
                "Access key of an AWS user with sufficient permissions. (See documentation)",
                ConfigurationField.Optional.NOT_OPTIONAL,
                TextField.Attribute.IS_PASSWORD
        ));

        r.addField(new TextField(
                CK_SECRET_KEY,
                "AWS secret key",
                "",
                "Secret key of an AWS user with sufficient permissions. (See documentation)",
                ConfigurationField.Optional.NOT_OPTIONAL,
                TextField.Attribute.IS_PASSWORD
        ));

        return r;
    }

    @Override
    public boolean isExclusive() {
        return false;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String linkToDocs() {
        return "";
    }

    @Override
    public Map<String, Object> getAttributes() {
        return configuration.getSource();
    }

    private boolean stringIsSet(String s) {
        return s != null && !s.trim().isEmpty();
    }

}
