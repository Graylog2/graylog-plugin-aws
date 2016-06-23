package org.graylog.aws.processors.instancelookup;

public class DiscoveredEC2Instance extends DiscoveredInstance {

    private final String name;

    public DiscoveredEC2Instance(String instanceId) {
        this.name = "ec2:" + instanceId;
    }

    @Override
    public String getName() {
        return name;
    }

}
