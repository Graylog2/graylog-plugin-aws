package org.graylog.aws.cloudwatch;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

public class CloudWatchLogEntry {

    private static final String LOG_GROUP = "log_group";
    private static final String LOG_STREAM = "log_stream";
    private static final String TIMESTAMP = "timestamp";
    private static final String MESSAGE = "message";

    @JsonProperty(LOG_GROUP)
    public String logGroup;

    @JsonProperty(LOG_STREAM)
    public String logStream;

    @JsonProperty(TIMESTAMP)
    public long timestamp;

    @JsonProperty(MESSAGE)
    public String message;

    public CloudWatchLogEntry() {
    }

    public CloudWatchLogEntry(String logGroup, String logStream, long timestamp, String message) {
        this.logGroup = logGroup;
        this.logStream = logStream;
        this.timestamp = timestamp;
        this.message = message;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add(LOG_GROUP, logGroup)
                          .add(LOG_STREAM, logStream)
                          .add(TIMESTAMP, timestamp)
                          .add(MESSAGE, message)
                          .toString();
    }
}