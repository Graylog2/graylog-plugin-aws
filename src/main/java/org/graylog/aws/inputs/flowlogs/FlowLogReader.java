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
import org.graylog.aws.config.AWSPluginConfiguration;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.journal.RawMessage;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

public class FlowLogReader implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(FlowLogReader.class);

    private final Region region;
    private final String groupName;
    private final MessageInput sourceInput;
    private final int backtrackLimit;
    private final int delayMinutes;

    private final AtomicBoolean paused;

    private final ClusterConfigService configService;

    public FlowLogReader(Region region,
                         String groupName,
                         MessageInput input,
                         int backtrackLimit,
                         int delayMinutes,
                         AtomicBoolean paused,
                         ClusterConfigService configService) {
        this.region = region;
        this.groupName = groupName;
        this.sourceInput = input;
        this.backtrackLimit = backtrackLimit;
        this.delayMinutes = delayMinutes;

        this.paused = paused;
        this.configService = configService;

        LOG.info("Starting AWS FlowLog reader with configured [{}m] delay.", this.delayMinutes);
    }

    // TODO metrics
    public void run() {
        // Don't run if underlying input is paused or not started yet.
        if(paused.get()) {
            LOG.debug("FlowLog reader is paused. Skipping this run.");
            return;
        }

        try {
            // Don't run if config is incomplete.
            AWSPluginConfiguration config = configService.get(AWSPluginConfiguration.class);
            if(config == null || !config.isComplete()) {
                LOG.warn("AWS plugin is not fully configured. Not reading FlowLogs.");
                return;
            }

            LOG.debug("Starting.");

            DateTime thisRunSecond = DateTime.now(DateTimeZone.UTC)
                    .withMillisOfSecond(0)
                    .minusMinutes(delayMinutes);

            DateTime lastRun = readLastRun();
            if (lastRun == null) {
                // If this is the first run or no last run info is available, read the latest 1 minute of data we can get.
                lastRun = thisRunSecond.minusMinutes(1);
            }

            // See if we need to limit backtracking based on user configuration
            Duration backtrack = new Duration(lastRun, thisRunSecond);
            if (backtrack.getStandardHours() > backtrackLimit) {
                LOG.warn("Last run was <{}> hours ago but backtrack limit is <{}> hours. Limiting.",
                        backtrack.getStandardHours(), backtrackLimit);

                lastRun = thisRunSecond.minusHours(backtrackLimit);
            }

            AWSLogsClient client = new AWSLogsClient(new BasicAWSCredentials(config.accessKey(), config.secretKey()));
            client.setRegion(region);

            // Get all flow log streams in this group. (TODO: support stream selection by prefix?)
            DescribeLogStreamsRequest req = new DescribeLogStreamsRequest(groupName);
            ImmutableList.Builder<LogStream> streamsBuilder = ImmutableList.<LogStream>builder();
            String nextToken = null;
            while (true) {
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
                    .withStartFromHead(true);

            LOG.debug("Fetching logs of stream [{}] with following request parameters: {}", buildFlowLogName(stream.getLogStreamName()), req.toString());

            // Iterate over pages that are available for this request.
            String nextToken = null;
            String previousToken;
            while (true) {
                if (nextToken != null) {
                    // Subsequent page call.
                    req.withNextToken(nextToken);
                }

                GetLogEventsResult logs = client.getLogEvents(req);
                int count = logs.getEvents().size();

                if (count > 0){
                    LOG.debug("Processing <{}> FlowLogs for page [{}] of slice [{}].",
                            count,
                            (nextToken == null ? "FIRST" : nextToken),
                            describeLogEventsRequest(req));

                    // Process all messages
                    for (OutputLogEvent log : logs.getEvents()) {
                        String message = new StringBuilder(log.getTimestamp().toString()).append(" ").append(log.getMessage()).toString();
                        sourceInput.processRawMessage(new RawMessage(message.getBytes()));
                    }
                } else {
                    LOG.debug("Page [{}] of FlowLogs slice [{}] is empty.",
                            (nextToken == null ? "FIRST" : nextToken),
                            describeLogEventsRequest(req));
                }

                previousToken = nextToken;
                nextToken = logs.getNextForwardToken();
                if (nextToken == null || nextToken.equals(previousToken)) {
                    break;
                }
            }
        } catch (Exception e) {
            LOG.error("Could not read AWS FlowLogs from stream [{}].", stream.getLogStreamName(), e);
        }
    }

    private void writeLastRun(DateTime thisRunSecond) {
        // TODO: fuck me this is clunky and I can smell them edge case race conditions
        AWSPluginConfiguration oldConf = configService.get(AWSPluginConfiguration.class);
        AWSPluginConfiguration newConf = AWSPluginConfiguration.create(
                oldConf.lookupsEnabled(),
                oldConf.lookupRegions(),
                oldConf.accessKey(),
                oldConf.secretKey(),
                thisRunSecond.toString()
        );

        configService.write(newConf);
    }

    private DateTime readLastRun() {
        AWSPluginConfiguration c = configService.get(AWSPluginConfiguration.class);

        if(c == null || c.flowlogsLastRun() == null || c.flowlogsLastRun().isEmpty()) {
            LOG.info("No lastRun state found in clusterConfig. This is OK if this is the first run on this node.");
            return null;
        } else {
            return DateTime.parse(c.flowlogsLastRun());
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

    private String describeLogEventsRequest(GetLogEventsRequest req) {
        return new StringBuilder()
                .append(buildFlowLogName(req.getLogStreamName()))
                .append("{").append(new DateTime(req.getStartTime()))
                .append("->")
                .append(new DateTime(req.getEndTime())).append("}")
                .toString();
    }


}
