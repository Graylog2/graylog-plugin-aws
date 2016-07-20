package org.graylog.aws.processors.instancelookup;

public abstract class DiscoveredInstance {

    public static final UndiscoveredInstance UNDISCOVERED = new UndiscoveredInstance(null);

    public abstract String getName();

    public abstract String getDescription();

    public abstract String getAWSType();

}
