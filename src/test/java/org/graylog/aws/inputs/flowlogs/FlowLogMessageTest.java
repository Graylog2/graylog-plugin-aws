package org.graylog.aws.inputs.flowlogs;

import org.graylog.aws.cloudwatch.CloudWatchLogEvent;
import org.graylog.aws.cloudwatch.FlowLogMessage;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class FlowLogMessageTest {

    @Test
    public void testFromPartsDoesNotFailWithMissingIntegerFields() throws Exception {
        final CloudWatchLogEvent logEvent = new CloudWatchLogEvent();
        final String[] strings = {
                "-",
                "foo",
                "eth0",
                "127.0.0.1",
                "127.0.0.1",
                "-",
                "-",
                "-",
                "100",
                "100",
                "0",
                "0",
                "ACCEPT",
                "OK"
        };
        logEvent.message = String.join(" ", strings);

        FlowLogMessage m = FlowLogMessage.fromLogEvent(logEvent);

        assertEquals(m.getDestinationPort(), 0);
        assertEquals(m.getSourcePort(), 0);
        assertEquals(m.getVersion(), 0);
        assertEquals(m.getProtocolNumber(), 0);
    }

    @Test
    public void testFromPartsDoesNotFailWithMissingLongFields() throws Exception {
        final String[] strings = {
                "1",
                "foo",
                "eth0",
                "127.0.0.1",
                "127.0.0.1",
                "80",
                "80",
                "1",
                "-",
                "-",
                "0",
                "0",
                "ACCEPT",
                "OK"
        };
        final CloudWatchLogEvent logEvent = new CloudWatchLogEvent();
        logEvent.message = String.join(" ", strings);
        FlowLogMessage m = FlowLogMessage.fromLogEvent(logEvent);

        assertEquals(m.getBytes(), 0);
        assertEquals(m.getPackets(), 0);
    }

}