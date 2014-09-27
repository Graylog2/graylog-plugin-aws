package com.graylog2.input;

import com.google.common.collect.Maps;
import com.graylog2.input.cloudtrail.files.S3Reader;
import com.graylog2.input.cloudtrail.notifications.CloudtrailSNSNotification;
import com.graylog2.input.cloudtrail.notifications.CloudtrailSQSSubscriber;
import org.graylog2.plugin.buffers.Buffer;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.MisfireException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class AWSInput extends MessageInput {

    private static final Logger LOG = LoggerFactory.getLogger(AWSInput.class);

    /*
     * TODO:
     *   * set up SNS/SQS bindings automatically
     *   * readCompressed credentials from config
     *   * allow to configure region
     *   * allow to configure queue name
     */

    // TODO
    public static final String ACCESS_KEY = "AKIAIFFKDVDZOL2NXCCA";
    public static final String SECRET_KEY = "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";

    private static final String NAME = "AWS Input";

    private boolean stopped = false;

    @Override
    public void checkConfiguration(Configuration configuration) throws ConfigurationException {
        return;
    }

    @Override
    public void launch(Buffer buffer) throws MisfireException {
        CloudtrailSQSSubscriber subscriber = new CloudtrailSQSSubscriber("cloudtrail-write");

        // TODO: use peridoc executor or shit
        while(!stopped) {
            System.out.println("checking for messages");

            // TODO readCompressed until there is nothing more to readCompressed, then sleep.

            for (CloudtrailSNSNotification n : subscriber.getNotifications()) {
                try {
                    System.out.println(S3Reader.readCompressed(n.getS3Bucket(), n.getS3ObjectKey()));
                } catch (Exception e) {
                    // TODO: what if the file just doesn't exist? separate between things that can be just skipped forever and stuff that needs to be retried.
                    LOG.error("Could not readCompressed CloudTrail log file for <{}>. Skipping.", n.getS3Bucket(), e);
                    continue;
                }
            }

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

        r.addField(new TextField(
            "foo",
            "foo",
            "foo",
            "REMOVE ME, AUTO SETUP!",
             ConfigurationField.Optional.OPTIONAL
        ));

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
