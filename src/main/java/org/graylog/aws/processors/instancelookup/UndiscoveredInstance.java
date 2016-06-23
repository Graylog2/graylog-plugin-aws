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

}
