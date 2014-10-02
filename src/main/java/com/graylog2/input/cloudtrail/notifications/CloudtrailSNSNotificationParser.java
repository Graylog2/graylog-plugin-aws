package com.graylog2.input.cloudtrail.notifications;

import com.amazonaws.services.sqs.model.Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.graylog2.input.cloudtrail.json.CloudtrailWriteNotification;
import com.graylog2.input.generic.json.SQSMessage;

import java.io.IOException;
import java.util.List;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class CloudtrailSNSNotificationParser {

    private final ObjectMapper om;

    public CloudtrailSNSNotificationParser() {
        om = new ObjectMapper();
    }

    public List<CloudtrailSNSNotification> parse(Message message) {
        List<CloudtrailSNSNotification> notifications = Lists.newArrayList();

        try {
            SQSMessage envelope = om.readValue(message.getBody(), SQSMessage.class);

            if (envelope.message == null) {
                return notifications;
            }

            CloudtrailWriteNotification notification = om.readValue(envelope.message, CloudtrailWriteNotification.class);

            for (String s3ObjectKey : notification.s3ObjectKey) {
                notifications.add(new CloudtrailSNSNotification(message.getReceiptHandle(), notification.s3Bucket, s3ObjectKey));
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not parse SNS notification: " + message.getBody(), e);
        }

        return notifications;
    }

}
