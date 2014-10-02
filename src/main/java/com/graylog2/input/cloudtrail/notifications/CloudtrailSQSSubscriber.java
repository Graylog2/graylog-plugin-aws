package com.graylog2.input.cloudtrail.notifications;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.*;
import com.google.common.collect.Lists;
import com.graylog2.input.AWSInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class CloudtrailSQSSubscriber {

    private static final Logger LOG = LoggerFactory.getLogger(AWSInput.class);

    private final AmazonSQS sqs;
    private final String queueName;

    public CloudtrailSQSSubscriber(String queueName) {
        AWSCredentials credentials = new BasicAWSCredentials(AWSInput.ACCESS_KEY, AWSInput.SECRET_KEY);

        this.sqs = new AmazonSQSClient(credentials);
        this.sqs.setRegion(Region.getRegion(Regions.EU_WEST_1)); // TODO

        this.queueName = queueName;
    }


    public List<CloudtrailSNSNotification> getNotifications() {
        LOG.debug("Fetching SQS CloudTrail notifications.");

        List<CloudtrailSNSNotification> notifications = Lists.newArrayList();

        ReceiveMessageRequest request = new ReceiveMessageRequest(queueName);
        request.setMaxNumberOfMessages(1);
        ReceiveMessageResult result = sqs.receiveMessage(request);

        LOG.debug("Received [{}] SQS CloudTrail notifications.", result.getMessages().size());

        CloudtrailSNSNotificationParser parser = new CloudtrailSNSNotificationParser();

        for (Message message : result.getMessages()) {
            notifications.addAll(parser.parse(message));
        }

        return notifications;
    }

    public void deleteNotification(CloudtrailSNSNotification notification) {
        LOG.debug("Deleting SQS CloudTrail notification <{}>.", notification.getReceiptHandle());

        sqs.deleteMessage(new DeleteMessageRequest()
                .withQueueUrl(queueName)
                .withReceiptHandle(notification.getReceiptHandle()));
    }
}
