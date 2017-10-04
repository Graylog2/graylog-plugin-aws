package org.graylog.aws.inputs.cloudtrail.notifications;

import com.amazonaws.services.sqs.model.Message;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.aws.inputs.cloudtrail.json.CloudtrailWriteNotification;
import org.graylog.aws.json.SQSMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CloudtrailSNSNotificationParser {
    private final ObjectMapper objectMapper = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    public List<CloudtrailSNSNotification> parse(Message message) {

        try {
            final SQSMessage envelope = objectMapper.readValue(message.getBody(), SQSMessage.class);

            if (envelope.message == null) {
                return Collections.emptyList();
            }

            final CloudtrailWriteNotification notification = objectMapper.readValue(envelope.message, CloudtrailWriteNotification.class);

            final List<String> s3ObjectKeys = notification.s3ObjectKey;
            if (s3ObjectKeys == null) {
                return Collections.emptyList();
            }

            final List<CloudtrailSNSNotification> notifications = new ArrayList<>(s3ObjectKeys.size());
            for (String s3ObjectKey : s3ObjectKeys) {
                notifications.add(new CloudtrailSNSNotification(message.getReceiptHandle(), notification.s3Bucket, s3ObjectKey));
            }
            return notifications;
        } catch (IOException e) {
            throw new RuntimeException("Could not parse SNS notification: " + message.getBody(), e);
        }
    }
}
