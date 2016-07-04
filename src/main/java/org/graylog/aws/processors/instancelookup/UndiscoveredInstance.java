package org.graylog.aws.processors.instancelookup;

public class UndiscoveredInstance extends DiscoveredInstance {

    private final String name;

    public UndiscoveredInstance(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public String getAWSType() {
        return null;
    }


}
