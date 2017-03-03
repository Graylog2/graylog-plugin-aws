package org.graylog.aws.plugin;

import com.google.auto.service.AutoService;
import org.graylog2.plugin.Plugin;
import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.PluginModule;

import java.util.Collection;
import java.util.Collections;

@AutoService(Plugin.class)
public class AWSPlugin implements Plugin {
    @Override
    public Collection<PluginModule> modules() {
        return Collections.singleton(new AWSModule());
    }

    @Override
    public PluginMetaData metadata() {
        return new AWSPluginMetadata();
    }
}
