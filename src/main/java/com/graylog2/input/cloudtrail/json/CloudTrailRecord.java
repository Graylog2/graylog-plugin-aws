package com.graylog2.input.cloudtrail.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.Maps;

import java.util.Arrays;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CloudTrailRecord {

    public String eventVersion;
    public String eventTime;

    public CloudTrailUserIdentity userIdentity;

    public String eventSource;
    public String eventName;
    public String awsRegion;
    public String sourceIPAddress;
    public String userAgent;
    public String requestID;
    public String eventID;
    public String eventType;
    public String recipientAccountId;
    public Map<String, Object> requestParameters;

    public Map<String, Object> additionalFieldsAsMap() {
        Map<String, Object> m = Maps.newHashMap();

        m.put("event_source", eventSource);
        m.put("event_name", eventName);
        m.put("aws_region", awsRegion);
        m.put("source_address", sourceIPAddress);
        m.put("user_agent", userAgent);
        m.put("request_id", requestID);
        m.put("event_id", eventID);
        m.put("event_type", eventType);
        m.put("recipient_account_id", recipientAccountId);

        if (userIdentity != null) {
            m.putAll(userIdentity.additionalFieldsAsMap());
        }

        return m;
    }

    public String getFullMessage() {
        if(requestParameters != null && !requestParameters.isEmpty()) {
            // Le pretty print.
            return Arrays.toString(requestParameters.entrySet().toArray());
        }

        return null;
    }

    public String getConstructedMessage() {
        StringBuilder sb = new StringBuilder();

        sb.append(eventSource).append(":").append(eventName).append(" in ").append(awsRegion)
                .append(" by ").append(sourceIPAddress).append(" / ").append(userIdentity.userName);

        return sb.toString();
    }

}
