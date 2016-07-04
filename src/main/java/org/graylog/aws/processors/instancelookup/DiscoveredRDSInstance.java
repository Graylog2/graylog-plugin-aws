package org.graylog.aws.processors.instancelookup;

import javax.annotation.Nullable;

public class DiscoveredRDSInstance extends DiscoveredInstance {

    private final String name;
    private final String description;

    public DiscoveredRDSInstance(@Nullable String name, @Nullable String description) {
        this.name = name;
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
        return "RDS";
    }

}
