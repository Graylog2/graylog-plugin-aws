package org.graylog.aws.inputs.transports;

public enum KinesisTransportState {
    STARTING,
    RUNNING,
    STOPPING,
    STOPPED
}
