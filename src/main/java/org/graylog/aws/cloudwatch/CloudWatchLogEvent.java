package org.graylog.aws.cloudwatch;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

public class CloudWatchLogEvent {
    @JsonProperty("timestamp")
    public long timestamp;

    @JsonProperty("message")
    public String message;

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("timestamp", timestamp)
                .add("message", message)
                .toString();
    }
}
