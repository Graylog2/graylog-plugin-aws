package org.graylog.aws.inputs.cloudtrail.notifications;

import com.amazonaws.services.sqs.model.Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.aws.inputs.cloudtrail.json.CloudtrailWriteNotification;
import org.graylog.aws.json.SQSMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CloudtrailSNSNotificationParser {
    private final ObjectMapper objectMapper;
    private static final Logger LOG = LoggerFactory.getLogger(CloudtrailSNSNotificationParser.class);

    public CloudtrailSNSNotificationParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<CloudtrailSNSNotification> parse(Message message) {

        LOG.debug("Parsing message.");
        try {
            LOG.debug("Reading message body {}.", message.getBody());
            final SQSMessage envelope = objectMapper.readValue(message.getBody(), SQSMessage.class);

            if (envelope.message == null) {
                LOG.warn("Message is empty. Processing of message has been aborted. Verify that the SQS subscription in AWS is NOT set to send raw data.");
                return Collections.emptyList();
            }

            LOG.debug("Reading message envelope {}.", envelope.message);
            final CloudtrailWriteNotification notification = objectMapper.readValue(envelope.message, CloudtrailWriteNotification.class);

            final List<String> s3ObjectKeys = notification.s3ObjectKey;
            if (s3ObjectKeys == null) {
                LOG.debug("No S3 object keys parsed.");
                return Collections.emptyList();
            }

            LOG.debug("Processing [{}] S3 keys.", s3ObjectKeys.size());
            final List<CloudtrailSNSNotification> notifications = new ArrayList<>(s3ObjectKeys.size());
            for (String s3ObjectKey : s3ObjectKeys) {
                notifications.add(new CloudtrailSNSNotification(message.getReceiptHandle(), notification.s3Bucket, s3ObjectKey));
            }

            LOG.debug("Returning [{}] notifications.", notifications.size());
            return notifications;
        } catch (IOException e) {
            LOG.error("Parsing exception.", e);
            /* Don't throw an exception that would halt processing for one parsing failure.
             * Sometimes occasional non-JSON test messages will come through. If this happens,
             * just log the error and keep processing.
              *
              * Returning an empty list here is OK and should be caught by the caller. */
            return new ArrayList<>();
        }
    }
}