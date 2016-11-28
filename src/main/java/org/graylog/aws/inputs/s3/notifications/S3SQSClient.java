package org.graylog.aws.inputs.s3.notifications;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.retry.RetryPolicy;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.*;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class S3SQSClient {
    private static final Logger LOG = LoggerFactory.getLogger(S3SQSClient.class);

    private final AmazonSQS sqs;
    private final String queueName;

    public S3SQSClient(Region region, String queueName, String accessKey, String secretKey) {
        ClientConfiguration clientConfiguration = new ClientConfiguration();

        clientConfiguration.setRequestTimeout(5000);
        clientConfiguration.setRetryPolicy(new RetryPolicy(null, null, 3, true));

        if (accessKey.isEmpty() && secretKey.isEmpty()) {
            this.sqs = new AmazonSQSClient(new InstanceProfileCredentialsProvider(), clientConfiguration);
        } else {
            this.sqs = new AmazonSQSClient(new BasicAWSCredentials(accessKey, secretKey), clientConfiguration);
        }
        this.sqs.setRegion(region);

        this.queueName = queueName;
    }

    public List<S3SNSNotification> getNotifications() {
        LOG.debug("Fetching SQS S3 notifications from " + queueName);

        List<S3SNSNotification> notifications = Lists.newArrayList();

        ReceiveMessageRequest request = new ReceiveMessageRequest(queueName);
        request.setMaxNumberOfMessages(10);
        ReceiveMessageResult result = sqs.receiveMessage(request);

        LOG.debug("Received [{}] SQS S3 notifications.", result.getMessages().size());

        S3SNSNotificationParser parser = new S3SNSNotificationParser();

        for (Message message : result.getMessages()) {
            notifications.addAll(parser.parse(message));
        }

        return notifications;
    }

    public void deleteNotifications(List<S3SNSNotification> notifications) {
        LOG.debug("Deleting " + notifications.size() + " notifications in batch request");
        List<DeleteMessageBatchRequestEntry> deleteMessageBatchRequestEntries = new ArrayList<>();
        for (S3SNSNotification n : notifications) {
            deleteMessageBatchRequestEntries.add(new DeleteMessageBatchRequestEntry(UUID.randomUUID().toString(), n.getReceiptHandle()));
        }
        DeleteMessageBatchRequest request = new DeleteMessageBatchRequest(queueName);
        request.withEntries(deleteMessageBatchRequestEntries);
        sqs.deleteMessageBatch(request);
    }

}
