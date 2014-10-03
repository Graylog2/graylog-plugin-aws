package com.graylog2.input;

import com.amazonaws.regions.Region;
import com.graylog2.input.cloudtrail.messages.TreeReader;
import com.graylog2.input.cloudtrail.notifications.CloudtrailSNSNotification;
import com.graylog2.input.cloudtrail.notifications.CloudtrailSQSClient;
import com.graylog2.input.s3.S3Reader;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.buffers.Buffer;
import org.graylog2.plugin.inputs.MessageInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class CloudTrailSubscriber extends Thread {

    private static final Logger LOG = LoggerFactory.getLogger(CloudTrailSubscriber.class);

    public static final int SLEEP_INTERVAL_SECS = 5;

    private boolean stopped = false;

    private final MessageInput sourceInput;

    private final Region region;
    private final String queueName;
    private final Buffer buffer;
    private final String accessKey;
    private final String secretKey;

    public CloudTrailSubscriber(Region region, String queueName, Buffer buffer, String accessKey, String secretKey, MessageInput sourceInput) {
        this.region = region;
        this.queueName = queueName;
        this.buffer = buffer;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.sourceInput = sourceInput;
    }

    @Override
    public void run() {
        CloudtrailSQSClient subscriber = new CloudtrailSQSClient(
                region,
                queueName,
                accessKey,
                secretKey
        );

        TreeReader reader = new TreeReader();
        S3Reader s3Reader = new S3Reader(accessKey, secretKey);

        while(!stopped) {
            while(!stopped) {
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
                                s3Reader.readCompressed(
                                        region,
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
                            buffer.insertCached(message, sourceInput);
                        }

                        // All messages written. Ack notification.
                        subscriber.deleteNotification(n);
                    } catch (Exception e) {
                        LOG.error("Could not read CloudTrail log file for <{}>. Skipping.", n.getS3Bucket(), e);
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

    public void terminate() {
        stopped = true;
    }

}
