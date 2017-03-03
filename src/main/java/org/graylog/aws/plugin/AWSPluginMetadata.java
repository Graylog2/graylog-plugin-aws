package org.graylog.aws.plugin;

import com.google.common.collect.ImmutableSet;
import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.Version;

import java.net.URI;
import java.util.Set;

public class AWSPluginMetadata implements PluginMetaData {
    private static final String PLUGIN_PROPERTIES = "org.graylog.plugins.graylog-plugin-aws/graylog-plugin.properties";

    @Override
    public String getUniqueId() {
        return AWSPlugin.class.getCanonicalName();
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
        return Version.fromPluginProperties(getClass(), PLUGIN_PROPERTIES, "version", Version.from(1, 3, 0));
    }

    @Override
    public String getDescription() {
        return "Collection of plugins to read data from or interact with the Amazon Web Services (AWS).";
    }

    @Override
    public Version getRequiredVersion() {
        return Version.fromPluginProperties(getClass(), PLUGIN_PROPERTIES, "graylog.version", Version.from(2, 2, 0));
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
