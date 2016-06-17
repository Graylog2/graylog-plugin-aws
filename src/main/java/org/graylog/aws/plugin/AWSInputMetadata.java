package org.graylog.aws.plugin;

import com.google.common.collect.ImmutableSet;
import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.Version;

import java.net.URI;
import java.util.Set;

public class AWSInputMetadata implements PluginMetaData {

    @Override
    public String getUniqueId() {
        return AWSInputPlugin.class.getCanonicalName();
    }

    @Override
    public String getName() {
        return "AWS plugins";
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
        return new Version(1, 0, 0);
    }

    @Override
    public String getDescription() {
        return "Collection of plugins to read data from or interact with the Amazon Web Services (AWS).";
    }

    @Override
    public Version getRequiredVersion() {
        return new Version(2, 0, 0);
    }

    @Override
    public Set<ServerStatus.Capability> getRequiredCapabilities() {
        return new ImmutableSet.Builder<ServerStatus.Capability>()
                /*
                 * This plugin will only start on the graylog-server master node because we are
                 * mostly working with the AWS REST APIs and running on multiple graylog-server
                 * nodes would result in data duplication.
                 */
                .add(ServerStatus.Capability.MASTER)
                .build();
    }
}
