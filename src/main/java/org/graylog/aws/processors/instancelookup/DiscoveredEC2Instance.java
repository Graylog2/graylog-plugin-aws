package org.graylog.aws.processors.instancelookup;

import javax.annotation.Nullable;

public class DiscoveredEC2Instance extends DiscoveredInstance {

    private final String name;
    private final String description;

    public DiscoveredEC2Instance(String instanceId, @Nullable String description) {
        this.name = "ec2:" + instanceId;
        this.description = description;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getAWSType() {
        return "EC2";
    }

}
