package com.graylog2.input;

import com.google.common.collect.Maps;
import com.graylog2.input.cloudtrail.CloudtrailSQSSubscriber;
import org.graylog2.plugin.buffers.Buffer;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.MisfireException;

import java.util.Map;

public class AWSInput extends MessageInput {

    /*
     * TODO:
     *   * set up SNS/SQS bindings automatically
     *   * read credentials from config
     *   * allow to configure region
     *   * allow to configure queue name
     */

    private static final String NAME = "AWS Input";

    private boolean stopped = false;

    @Override
    public void checkConfiguration() throws ConfigurationException {
        // TODO
    }

    @Override
    public void launch(Buffer buffer) throws MisfireException {
        CloudtrailSQSSubscriber subscriber = new CloudtrailSQSSubscriber("cloudtrail-write");

        // TODO: use peridoc executor or shit
        while(!stopped) {
            System.out.println("checking for messages");

            subscriber.getMessages();

            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {}
        }
    }

    @Override
    public void stop() {
        this.stopped = true;
    }

    @Override
    public ConfigurationRequest getRequestedConfiguration() {
        ConfigurationRequest r  = new ConfigurationRequest();

        return r;
    }

    @Override
    public boolean isExclusive() {
        return false;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String linkToDocs() {
        return "";
    }

    @Override
    public Map<String, Object> getAttributes() {
        // TODO
        return Maps.newHashMap();
    }

}
