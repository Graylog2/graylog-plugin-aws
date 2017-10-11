package org.graylog.aws.json;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SQSMessage {
    @JsonProperty("Message")
    public String message;
    @JsonProperty("MessageId")
    public String messageId;
}
