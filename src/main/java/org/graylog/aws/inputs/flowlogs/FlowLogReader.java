package org.graylog.aws.inputs.flowlogs;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.services.logs.AWSLogsClient;
import com.amazonaws.services.logs.model.DescribeLogStreamsRequest;
import com.amazonaws.services.logs.model.DescribeLogStreamsResult;
import com.amazonaws.services.logs.model.GetLogEventsRequest;
import com.amazonaws.services.logs.model.GetLogEventsResult;
import com.amazonaws.services.logs.model.LogStream;
import com.amazonaws.services.logs.model.OutputLogEvent;
import com.google.common.collect.ImmutableList;
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
    private final MessageInput sourceInput;
    private final String accessKey;
    private final String secretKey;

    private final AtomicBoolean paused;

    // TODO read from tmp file in every run() (remove this member)
    DateTime lastRun;

    public FlowLogReader(Region region, String groupName, MessageInput input, String accessKey, String secretKey, AtomicBoolean paused) {
        this.region = region;
        this.groupName = groupName;
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

        // TODO make offset configurable.
        DateTime thisRunSecond = DateTime.now(DateTimeZone.UTC).withMillisOfSecond(0).minusMinutes(15);

        // TODO fock fock what if the runs takes longer than 15 sec? this srsly needs a lastCall thingy
        if(lastRun == null) {
            lastRun = thisRunSecond.minusSeconds(15);
        }

        // TODO metrics

        AWSLogsClient client = new AWSLogsClient(new BasicAWSCredentials(accessKey, secretKey));
        client.setRegion(region);

        // Get all flow log streams in this group. (TODO: support stream selection by prefix?)
        ImmutableList.Builder<LogStream> streamsBuilder = ImmutableList.<LogStream>builder();
        String nextToken = null;
        while(true) {
            DescribeLogStreamsRequest req = new DescribeLogStreamsRequest(groupName);

            if(nextToken != null) {
                // Subsequent page call.
                req.withNextToken(nextToken);
            }

            DescribeLogStreamsResult streamsPage = client.describeLogStreams(req);

            // Add page to full result list.
            streamsBuilder.addAll(streamsPage.getLogStreams());

            nextToken = streamsPage.getNextToken();
            if(nextToken == null) {
                // Done with cycling through pages. We have all results.
                break;
            }
        }

        ImmutableList<LogStream> streams = streamsBuilder.build();

        for (LogStream stream : streams) {
            LOG.debug("Reading FlowLogs [{}] from [{}] to [{}].", buildFlowLogName(stream.getLogStreamName()), lastRun, thisRunSecond);

            try {
                GetLogEventsRequest req = new GetLogEventsRequest()
                        .withLogGroupName(groupName)
                        .withLogStreamName(stream.getLogStreamName())
                        .withStartTime(lastRun.getMillis())
                        .withEndTime(thisRunSecond.getMillis())
                        .withLimit(10_000)
                        .withStartFromHead(true);

                LOG.debug("Fetching logs of stream [{}] with following request parameters: {}", buildFlowLogName(stream.getLogStreamName()), req.toString());

                GetLogEventsResult logs = client.getLogEvents(req);

                // Iterate over all logs.
                for (OutputLogEvent log : logs.getEvents()) {
                    String message = new StringBuilder(log.getTimestamp().toString()).append(" ").append(log.getMessage()).toString();
                    sourceInput.processRawMessage(new RawMessage(message.getBytes()));
                }
            } catch (Exception e) {
                LOG.error("Could not read AWS FlowLogs.", e);
            }
        }

        // TODO persist in tmp sfile
        this.lastRun = thisRunSecond;
    }

    private String buildFlowLogName(String streamName) {
        return new StringBuilder()
                .append(region)
                .append("#")
                .append(groupName)
                .append(":")
                .append(streamName)
                .toString();
    }

}
