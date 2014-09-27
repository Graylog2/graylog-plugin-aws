package com.graylog2.plugin;

import java.util.Collection;
import com.google.common.collect.Lists;
import org.graylog2.plugin.Plugin;
import org.graylog2.plugin.PluginModule;

public class AWSInputPlugin implements Plugin {

    @Override
    public Collection<PluginModule> modules () {
        return Lists.newArrayList((PluginModule) new AWSInputModule());
    }

}
