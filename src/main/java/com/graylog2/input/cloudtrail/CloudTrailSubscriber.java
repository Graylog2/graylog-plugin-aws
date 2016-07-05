package com.graylog2.input.cloudtrail;

import com.amazonaws.regions.Region;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.Uninterruptibles;
import com.graylog2.input.cloudtrail.json.CloudTrailRecord;
import com.graylog2.input.cloudtrail.messages.TreeReader;
import com.graylog2.input.cloudtrail.notifications.CloudtrailSNSNotification;
import com.graylog2.input.cloudtrail.notifications.CloudtrailSQSClient;
import com.graylog2.input.s3.S3Reader;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.journal.RawMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class CloudTrailSubscriber extends Thread {
    private static final Logger LOG = LoggerFactory.getLogger(CloudTrailSubscriber.class);

    public static final int SLEEP_INTERVAL_SECS = 5;

    private volatile boolean stopped = false;
    private volatile boolean paused = false;
    private volatile CountDownLatch pausedLatch = new CountDownLatch(0);

    private final MessageInput sourceInput;

    private final Region sqsRegion;
    private final Region s3Region;
    private final String queueName;
    private final String accessKey;
    private final String secretKey;

    public CloudTrailSubscriber(Region sqsRegion, Region s3Region, String queueName, MessageInput sourceInput, String accessKey, String secretKey) {
        this.sqsRegion = sqsRegion;
        this.s3Region = s3Region;
        this.queueName = queueName;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.sourceInput = sourceInput;
    }

    public void pause() {
        paused = true;
        pausedLatch = new CountDownLatch(1);
    }

    // A ridiculous name because "resume" is already defined in the super class...
    public void unpause() {
        paused = false;
        pausedLatch.countDown();
    }

    @Override
    public void run() {
        CloudtrailSQSClient subscriber = new CloudtrailSQSClient(
                sqsRegion,
                queueName,
                accessKey,
                secretKey
        );

        final ObjectMapper objectMapper = new ObjectMapper();
        TreeReader reader = new TreeReader();
        S3Reader s3Reader = new S3Reader(accessKey, secretKey);

        while (!stopped) {
            while (!stopped) {
                if (paused) {
                    LOG.debug("Processing paused");
                    Uninterruptibles.awaitUninterruptibly(pausedLatch);
                }
                if (stopped) {
                    break;
                }

                List<CloudtrailSNSNotification> notifications;
                try {
                    notifications = subscriber.getNotifications();
                } catch (Exception e) {
                    LOG.error("Could not read messages from SNS. This is most likely a misconfiguration of the plugin. Going into sleep loop and retrying.", e);
                    break;
                }

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

                        List<CloudTrailRecord> records = reader.read(
                                s3Reader.readCompressed(
                                        s3Region,
                                        n.getS3Bucket(),
                                        n.getS3ObjectKey()
                                )
                        );

                        for (CloudTrailRecord record : records) {
                            /*
                             * We are using process and not processFailFast here even though we are using a
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

                            if (LOG.isTraceEnabled()) {
                                LOG.trace("Processing cloud trail record: {}", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(record));
                            }

                            sourceInput.processRawMessage(new RawMessage(objectMapper.writeValueAsBytes(record)));
                        }

                        // All messages written. Ack notification.
                        subscriber.deleteNotification(n);
                    } catch (Exception e) {
                        LOG.error("Could not read CloudTrail log file for <{}>. Skipping.", n.getS3Bucket(), e);
                    }
                }
            }

            if (!stopped) {
                LOG.debug("Waiting {} seconds until next CloudTrail SQS check.", SLEEP_INTERVAL_SECS);
                Uninterruptibles.sleepUninterruptibly(SLEEP_INTERVAL_SECS, TimeUnit.SECONDS);
            }
        }
    }

    public void terminate() {
        stopped = true;
        paused = false;
        pausedLatch.countDown();
    }
}
