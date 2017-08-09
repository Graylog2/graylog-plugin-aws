package org.graylog.aws.cloudwatch;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class CloudWatchLogData {
    @JsonProperty("logEvents")
    public List<CloudWatchLogEvent> logEvents;
}
