package org.graylog.aws.processors.instancelookup;

import javax.annotation.Nullable;

public class DiscoveredELBInstance extends DiscoveredInstance {

    private final String name;
    private final String description;

    public DiscoveredELBInstance(String instanceId, @Nullable String description) {
        this.name = "elb:" + instanceId;
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
        return "ELB";
    }

}
