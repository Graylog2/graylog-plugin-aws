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
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.graylog.aws.tools.StateFile;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.journal.RawMessage;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class FlowLogReader implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(FlowLogReader.class);

    private final Region region;
    private final String groupName;
    private final MessageInput sourceInput;
    private final String accessKey;
    private final String secretKey;

    private final StateFile statefile;

    private final AtomicBoolean paused;

    public FlowLogReader(Region region, String groupName, MessageInput input, String accessKey, String secretKey, AtomicBoolean paused) {
        this.region = region;
        this.groupName = groupName;
        this.sourceInput = input;
        this.accessKey = accessKey;
        this.secretKey = secretKey;

        this.statefile = new StateFile("flowlog_last_read");

        this.paused = paused;
    }

    // TODO metrics
    public void run() {
        // Don't run if underlying input is paused or not started yet.
        if(paused.get()) {
            LOG.debug("FlowLog reader is paused. Skipping this run.");
            return;
        }

        try {
            LOG.debug("Starting.");

            // TODO make offset configurable?
            DateTime thisRunSecond = DateTime.now(DateTimeZone.UTC)
                    .withMillisOfSecond(0)
                    .minusMinutes(15);


            DateTime lastRun = readLastRun();
            if (lastRun == null) {
                // If this is the first run or no last run info is available, read the latest 1 minute of data we can get.
                lastRun = thisRunSecond.minusMinutes(1);
            }

            AWSLogsClient client = new AWSLogsClient(new BasicAWSCredentials(accessKey, secretKey));
            client.setRegion(region);

            // Get all flow log streams in this group. (TODO: support stream selection by prefix?)
            ImmutableList.Builder<LogStream> streamsBuilder = ImmutableList.<LogStream>builder();
            String nextToken = null;
            while (true) {
                DescribeLogStreamsRequest req = new DescribeLogStreamsRequest(groupName);

                if (nextToken != null) {
                    // Subsequent page call.
                    req.withNextToken(nextToken);
                }

                DescribeLogStreamsResult streamsPage = client.describeLogStreams(req);

                // Add page to full result list.
                streamsBuilder.addAll(streamsPage.getLogStreams());

                nextToken = streamsPage.getNextToken();
                if (nextToken == null) {
                    // Done with cycling through pages. We have all results.
                    break;
                }
            }

            ImmutableList<LogStream> streams = streamsBuilder.build();

            for (LogStream stream : streams) {
                readStream(thisRunSecond, lastRun, client, stream);
            }

            writeLastRun(thisRunSecond);
        } catch(Exception e) {
            LOG.error("AWS FlowLog reader run failed.", e);
        }
    }

    private void readStream(DateTime end, DateTime start, AWSLogsClient client, LogStream stream) {
        LOG.debug("Reading FlowLogs [{}] from [{}] to [{}].", buildFlowLogName(stream.getLogStreamName()), start, end);

        try {
            GetLogEventsRequest req = new GetLogEventsRequest()
                    .withLogGroupName(groupName)
                    .withLogStreamName(stream.getLogStreamName())
                    .withStartTime(start.getMillis())
                    .withEndTime(end.getMillis())
                    .withLimit(10_000) // TODO read smaller batches and use paging
                    .withStartFromHead(true);

            LOG.debug("Fetching logs of stream [{}] with following request parameters: {}", buildFlowLogName(stream.getLogStreamName()), req.toString());

            GetLogEventsResult logs = client.getLogEvents(req);

            // Iterate over all logs.
            for (OutputLogEvent log : logs.getEvents()) {
                String message = new StringBuilder(log.getTimestamp().toString()).append(" ").append(log.getMessage()).toString();
                sourceInput.processRawMessage(new RawMessage(message.getBytes()));
            }
        } catch (Exception e) {
            LOG.error("Could not read AWS FlowLogs from stream [{}].", stream.getLogStreamName(), e);
        }
    }

    private void writeLastRun(DateTime thisRunSecond) throws IOException {
        statefile.writeValue(thisRunSecond.toString());
    }

    private DateTime readLastRun() throws IOException {
        String result = statefile.readValue();
        if(result == null) {
            LOG.info("No state file found. This is OK if this is the first run.");
            return null;
        } else {
            return DateTime.parse(result);
        }
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
