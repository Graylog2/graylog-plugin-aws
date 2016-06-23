package org.graylog.aws.inputs.flowlogs;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.services.logs.AWSLogsClient;
import com.amazonaws.services.logs.model.GetLogEventsRequest;
import com.amazonaws.services.logs.model.GetLogEventsResult;
import com.amazonaws.services.logs.model.OutputLogEvent;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.journal.RawMessage;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

public class FlowLogReader implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(FlowLogReader.class);

    private final Region region;
    private final String groupName;
    private final String streamName;
    private final MessageInput sourceInput;
    private final String accessKey;
    private final String secretKey;

    private final AtomicBoolean paused;

    // TODO read from tmp file in every run() (remove this member)
    DateTime lastRun;

    public FlowLogReader(Region region, String groupName, String streamName, MessageInput input, String accessKey, String secretKey, AtomicBoolean paused) {
        this.region = region;
        this.groupName = groupName;
        this.streamName = streamName;
        this.sourceInput = input;
        this.accessKey = accessKey;
        this.secretKey = secretKey;

        this.paused = paused;
    }

    public void run() {
        // Don't run if underlying input is paused or not started yet.
        if(paused.get()) {
            LOG.debug("FlowLog reader is paused. Skipping this run.");
            return;
        }

        LOG.debug("Starting.");

        DateTime thisRunSecond = DateTime.now(DateTimeZone.UTC).withMillisOfSecond(0).minusMinutes(30);

        if(lastRun == null) {
            lastRun = thisRunSecond.minusSeconds(15);
        }

        // TODO make debug
        LOG.debug("Reading FlowLogs [{}] from [{}] to [{}].", buildFlowLogName(), lastRun, thisRunSecond);

        // TODO debug logs and metrics

        try {
            GetLogEventsRequest req = new GetLogEventsRequest()
                    .withLogGroupName(groupName)
                    .withLogStreamName(streamName)
                    .withStartTime(lastRun.getMillis())
                    .withEndTime(thisRunSecond.getMillis())
                    .withLimit(10_000)
                    .withStartFromHead(true);

            LOG.debug("Fetching logs with following request parameters: {}", req.toString());

            AWSLogsClient client = new AWSLogsClient(new BasicAWSCredentials(accessKey, secretKey));
            client.setRegion(region);
            GetLogEventsResult logs = client.getLogEvents(req);

            for (OutputLogEvent log : logs.getEvents()) {
                // TODO directly append bytes here
                String message = new StringBuilder(log.getTimestamp().toString()).append(" ").append(log.getMessage()).toString();
                sourceInput.processRawMessage(new RawMessage(message.getBytes()));
            }

            // TODO persist in tmp sfile
            this.lastRun = thisRunSecond;
        } catch(Exception e) {
            LOG.error("Could not read AWS FlowLogs.", e);
        }
    }

    private final String buildFlowLogName() {
        return new StringBuilder()
                .append(region)
                .append("#")
                .append(groupName)
                .append(":")
                .append(streamName)
                .toString();
    }

}
