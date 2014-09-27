package com.graylog2.plugin;

import com.graylog2.input.InputVersion;
import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.Version;

import java.net.URI;
import java.net.URISyntaxException;

public class AWSInputMetadata implements PluginMetaData {

    @Override
    public String getUniqueId() {
        /*
         * I have no idea what I'm doing. What is a
         * "unique ID" in this context? using a UUID
         * for now.
         */
        return "4c1d9e67-f481-4a92-a910-155116bd0fb5";
    }

    @Override
    public String getName() {
        return "AWS services input";
    }

    @Override
    public String getAuthor() {
        return "Lennart Koopmann";
    }

    @Override
    public URI getURL() {
        try {
            return new URI("http://www.graylog2.com/");
        } catch (URISyntaxException ignored) {
            throw new RuntimeException("Invalid plugin source URI.", ignored);
        }
    }

    @Override
    public Version getVersion() {
        return InputVersion.PLUGIN_VERSION;
    }

    @Override
    public String getDescription() {
        return "Prototype plugin to readCompressed log data from Amazon Web Services.";
    }

    @Override
    public Version getRequiredVersion() {
        return InputVersion.REQUIRED_VERSION;
    }

}
