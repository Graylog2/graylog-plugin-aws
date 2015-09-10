package com.graylog2.plugin;

import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.Version;

import java.net.URI;
import java.util.Collections;
import java.util.Set;

public class AWSInputMetadata implements PluginMetaData {

    @Override
    public String getUniqueId() {
        return AWSInputPlugin.class.getCanonicalName();
    }

    @Override
    public String getName() {
        return "AWS input";
    }

    @Override
    public String getAuthor() {
        return "Graylog, Inc.";
    }

    @Override
    public URI getURL() {
        return URI.create("https://www.graylog.com/");
    }

    @Override
    public Version getVersion() {
        return new Version(0, 5, 1);
    }

    @Override
    public String getDescription() {
        return "Prototype plugin to read compressed log data from Amazon Web Services.";
    }

    @Override
    public Version getRequiredVersion() {
        return new Version(1, 0, 0);
    }

    @Override
    public Set<ServerStatus.Capability> getRequiredCapabilities() {
        return Collections.emptySet();
    }
}
