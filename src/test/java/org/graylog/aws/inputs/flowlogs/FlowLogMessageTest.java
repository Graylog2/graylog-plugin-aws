package org.graylog.aws.inputs.flowlogs;

import org.graylog.aws.cloudwatch.CloudWatchLogEntry;
import org.graylog.aws.cloudwatch.FlowLogMessage;
import org.joda.time.DateTime;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class FlowLogMessageTest {

    @Test
    public void testFromPartsDoesNotFailWithMissingIntegerFields() throws Exception {
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

        final CloudWatchLogEntry logEvent = new CloudWatchLogEntry("helloStream", "helloGroup", DateTime.now().getMillis() / 1000, String.join(" ", strings));
        final FlowLogMessage m = FlowLogMessage.fromLogEvent(logEvent);

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

        final CloudWatchLogEntry logEvent = new CloudWatchLogEntry("helloStream", "helloGroup", DateTime.now().getMillis() / 1000, String.join(" ", strings));
        final FlowLogMessage m = FlowLogMessage.fromLogEvent(logEvent);

        assertEquals(m.getBytes(), 0);
        assertEquals(m.getPackets(), 0);
    }
}