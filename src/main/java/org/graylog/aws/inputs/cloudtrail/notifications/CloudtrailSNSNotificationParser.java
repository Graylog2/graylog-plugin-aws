package org.graylog.aws.inputs.cloudtrail.notifications;

import com.amazonaws.services.sqs.model.Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.graylog.aws.inputs.cloudtrail.json.CloudtrailWriteNotification;
import org.graylog.aws.json.SQSMessage;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

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
                return Collections.emptyList();
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
