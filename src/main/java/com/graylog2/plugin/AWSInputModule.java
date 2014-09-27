package com.graylog2.plugin;

import com.graylog2.input.AWSInput;
import org.graylog2.plugin.PluginModule;

public class AWSInputModule extends PluginModule {

    @Override
    protected void configure() {
        registerPlugin(AWSInputMetadata.class);

        addMessageInput(AWSInput.class);
    }

}
