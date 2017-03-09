package org.graylog.aws.inputs.cloudtrail.notifications;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.google.common.collect.Lists;
import okhttp3.HttpUrl;
import org.graylog.aws.config.Proxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class CloudtrailSQSClient {
    private static final Logger LOG = LoggerFactory.getLogger(CloudtrailSQSClient.class);

    private final AmazonSQS sqs;
    private final String queueName;

    public CloudtrailSQSClient(Region region, String queueName, String accessKey, String secretKey, HttpUrl proxyUrl) {
        final AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);

        if(proxyUrl != null) {
            this.sqs = new AmazonSQSClient(credentials, Proxy.forAWS(proxyUrl));
        } else {
            this.sqs = new AmazonSQSClient(credentials);
        }

        this.sqs.setRegion(region);

        this.queueName = queueName;
    }

    public List<CloudtrailSNSNotification> getNotifications() {
        LOG.debug("Fetching SQS CloudTrail notifications.");

        List<CloudtrailSNSNotification> notifications = Lists.newArrayList();

        ReceiveMessageRequest request = new ReceiveMessageRequest(queueName);
        request.setMaxNumberOfMessages(10);
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
