package org.graylog.aws.inputs.flowlogs;

import org.testng.annotations.Test;

import static org.testng.Assert.*;


public class FlowLogMessageTest {

    @Test
    public void testFromPartsDoesNotFailWithMissingIntegerFields() throws Exception {
        FlowLogMessage m = FlowLogMessage.fromParts(
                new String[]{
                        "0",
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
                }
        );

        assertEquals(m.getDestinationPort(), 0);
        assertEquals(m.getSourcePort(), 0);
        assertEquals(m.getVersion(), 0);
        assertEquals(m.getProtocolNumber(), 0);
    }

    @Test
    public void testFromPartsDoesNotFailWithMissingLongFields() throws Exception {
        FlowLogMessage m = FlowLogMessage.fromParts(
                new String[]{
                        "0",
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
                }
        );

        assertEquals(m.getBytes(), 0);
        assertEquals(m.getPackets(), 0);
    }

}