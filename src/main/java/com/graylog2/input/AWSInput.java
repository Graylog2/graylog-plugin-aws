package com.graylog2.input;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.google.common.collect.Maps;
import org.graylog2.plugin.buffers.Buffer;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.DropdownField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.MisfireException;

import java.util.Map;

public class AWSInput extends MessageInput {

    private static final String NAME = "AWS CloudTrail Input";

    private static final String CK_AWS_REGION = "aws_region";
    private static final String CK_SQS_NAME = "aws_sqs_queue_name";
    private static final String CK_ACCESS_KEY = "aws_access_key";
    private static final String CK_SECRET_KEY = "aws_secret_key";

    private CloudTrailSubscriber subscriber;

    @Override
    public void checkConfiguration(Configuration configuration) throws ConfigurationException {
        if(!stringIsSet(CK_AWS_REGION) || !stringIsSet(CK_SQS_NAME)
                || !stringIsSet(CK_ACCESS_KEY) || !stringIsSet(CK_SECRET_KEY)) {
            throw new ConfigurationException("Not all required configuration fields are set.");
        }
    }

    @Override
    public void launch(Buffer buffer) throws MisfireException {
        subscriber = new CloudTrailSubscriber(
                Region.getRegion(Regions.fromName(configuration.getString(CK_AWS_REGION))),
                configuration.getString(CK_SQS_NAME),
                buffer,
                configuration.getString(CK_ACCESS_KEY),
                configuration.getString(CK_SECRET_KEY),
                this
        );

        subscriber.start();
    }

    @Override
    public void stop() {
        if (subscriber != null) {
            subscriber.terminate();
        }
    }

    @Override
    public ConfigurationRequest getRequestedConfiguration() {
        ConfigurationRequest r  = new ConfigurationRequest();

        Map<String, String> regions = Maps.newHashMap();
        for (Regions region : Regions.values()) {
            regions.put(region.getName(), region.toString());
        }

        r.addField(new DropdownField(
                CK_AWS_REGION,
                "AWS Region",
                Regions.US_EAST_1.getName(),
                regions,
                "The AWS region to read CloudTrail for. The configured SQS queue " +
                        "must also be located in this region.",
                ConfigurationField.Optional.NOT_OPTIONAL
        ));

        r.addField(new TextField(
                CK_SQS_NAME,
                "SQS queue name",
                "cloudtrail-notifications",
                "The SQS queue that SNS is writing CloudTrail notifications to.",
                ConfigurationField.Optional.NOT_OPTIONAL
        ));

        r.addField(new TextField(
                CK_ACCESS_KEY,
                "AWS access key",
                "",
                "Access key of an AWS user with sufficient permissions. (See documentation)",
                ConfigurationField.Optional.NOT_OPTIONAL,
                TextField.Attribute.IS_PASSWORD
        ));

        r.addField(new TextField(
                CK_SECRET_KEY,
                "AWS secret key",
                "",
                "Secret key of an AWS user with sufficient permissions. (See documentation)",
                ConfigurationField.Optional.NOT_OPTIONAL,
                TextField.Attribute.IS_PASSWORD
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
        return configuration.getSource();
    }

    private boolean stringIsSet(String s) {
        return s != null && !s.trim().isEmpty();
    }

}
