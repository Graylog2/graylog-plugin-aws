package org.graylog.aws.cloudwatch;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

/**
 * A single CloudWatch log event.
 * <p/>
 * Example payload:
 * <pre>
 * {
 *   "id": "33503748002479370955346306650196094071913271643270021120",
 *   "timestamp": 1502360020000,
 *   "message": "2 123456789 eni-aaaaaaaa 10.0.27.226 10.42.96.199 3604 17720 17 1 132 1502360020 1502360079 REJECT OK"
 * }
 * </pre>
 */
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
