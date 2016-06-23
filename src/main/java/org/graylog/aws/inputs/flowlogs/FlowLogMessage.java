package org.graylog.aws.inputs.flowlogs;

import org.joda.time.DateTime;

public class FlowLogMessage {

    private final DateTime timestamp;
    private final int version;
    private final String accountId;
    private final String interfaceId;
    private final String sourceAddress;
    private final String destinationAddress;
    private final int sourcePort;
    private final int destinationPort;
    private final int protocolNumber;
    private final long packets;
    private final long bytes;
    private final DateTime captureWindowStart;
    private final DateTime captureWindowEnd;
    private final String action;
    private final String logStatus;

    public FlowLogMessage(DateTime timestamp,
                          int version,
                          String accountId,
                          String interfaceId,
                          String sourceAddress,
                          String destinationAddress,
                          int sourcePort,
                          int destinationPort,
                          int protocolNumber,
                          long packets,
                          long bytes,
                          DateTime captureWindowStart,
                          DateTime captureWindowEnd,
                          String action,
                          String logStatus) {
        this.timestamp = timestamp;
        this.version = version;
        this.accountId = accountId;
        this.interfaceId = interfaceId;
        this.sourceAddress = sourceAddress;
        this.destinationAddress = destinationAddress;
        this.sourcePort = sourcePort;
        this.destinationPort = destinationPort;
        this.protocolNumber = protocolNumber;
        this.packets = packets;
        this.bytes = bytes;
        this.captureWindowStart = captureWindowStart;
        this.captureWindowEnd = captureWindowEnd;
        this.action = action;
        this.logStatus = logStatus;
    }

    public static FlowLogMessage fromParts(String[] parts) {
        if(parts == null || parts.length != 15) {
            throw new IllegalArgumentException("Message parts were null or not length of 15");
        }
        return new FlowLogMessage(
                new DateTime(Long.valueOf(parts[0])),
                Integer.valueOf(parts[1]),
                parts[2],
                parts[3],
                parts[4],
                parts[5],
                Integer.valueOf(parts[6]),
                Integer.valueOf(parts[7]),
                Integer.valueOf(parts[8]),
                Long.valueOf(parts[9]),
                Long.valueOf(parts[10]),
                new DateTime(Long.valueOf(parts[11])*1000),
                new DateTime(Long.valueOf(parts[12])*1000),
                parts[13],
                parts[14]
        );
    }

    public DateTime getTimestamp() {
        return timestamp;
    }

    public int getVersion() {
        return version;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getInterfaceId() {
        return interfaceId;
    }

    public String getSourceAddress() {
        return sourceAddress;
    }

    public String getDestinationAddress() {
        return destinationAddress;
    }

    public int getSourcePort() {
        return sourcePort;
    }

    public int getDestinationPort() {
        return destinationPort;
    }

    public int getProtocolNumber() {
        return protocolNumber;
    }

    public long getPackets() {
        return packets;
    }

    public long getBytes() {
        return bytes;
    }

    public DateTime getCaptureWindowStart() {
        return captureWindowStart;
    }

    public DateTime getCaptureWindowEnd() {
        return captureWindowEnd;
    }

    public String getAction() {
        return action;
    }

    public String getLogStatus() {
        return logStatus;
    }

}
