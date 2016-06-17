package org.graylog.aws.plugin;

import org.graylog2.plugin.Plugin;
import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.PluginModule;

import java.util.Collection;
import java.util.Collections;

public class AWSInputPlugin implements Plugin {
    @Override
    public Collection<PluginModule> modules() {
        return Collections.<PluginModule>singleton(new AWSInputModule());
    }

    @Override
    public PluginMetaData metadata() {
        return new AWSInputMetadata();
    }
}
