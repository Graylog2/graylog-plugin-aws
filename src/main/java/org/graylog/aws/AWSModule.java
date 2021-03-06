/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.aws;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.aws.config.AWSConfigurationResource;
import org.graylog.aws.inputs.cloudtrail.CloudTrailCodec;
import org.graylog.aws.inputs.cloudtrail.CloudTrailInput;
import org.graylog.aws.inputs.cloudtrail.CloudTrailTransport;
import org.graylog.aws.inputs.cloudwatch.CloudWatchLogsInput;
import org.graylog.aws.inputs.codecs.CloudWatchFlowLogCodec;
import org.graylog.aws.inputs.codecs.CloudWatchRawLogCodec;
import org.graylog.aws.inputs.flowlogs.FlowLogsInput;
import org.graylog.aws.inputs.transports.KinesisTransport;
import org.graylog.aws.migrations.V20200505121200_EncryptAWSSecretKey;
import org.graylog.aws.processors.instancelookup.AWSInstanceNameLookupProcessor;
import org.graylog.aws.processors.instancelookup.InstanceLookupTable;
import org.graylog2.plugin.PluginModule;

public class AWSModule extends PluginModule {
    @Override
    protected void configure() {
        // CloudTrail
        addCodec(CloudTrailCodec.NAME, CloudTrailCodec.class);
        addTransport(CloudTrailTransport.NAME, CloudTrailTransport.class);
        addMessageInput(CloudTrailInput.class);

        // CloudWatch
        addCodec(CloudWatchFlowLogCodec.NAME, CloudWatchFlowLogCodec.class);
        addCodec(CloudWatchRawLogCodec.NAME, CloudWatchRawLogCodec.class);
        addTransport(KinesisTransport.NAME, KinesisTransport.class);
        addMessageInput(FlowLogsInput.class);
        addMessageInput(CloudWatchLogsInput.class);

        // Instance name lookup
        addMessageProcessor(AWSInstanceNameLookupProcessor.class, AWSInstanceNameLookupProcessor.Descriptor.class);

        bind(InstanceLookupTable.class).asEagerSingleton();
        bind(ObjectMapper.class).annotatedWith(AWSObjectMapper.class).toInstance(createObjectMapper());

        addMigration(V20200505121200_EncryptAWSSecretKey.class);
        addRestResource(AWSConfigurationResource.class);
    }

    private ObjectMapper createObjectMapper() {
        return new ObjectMapper()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }
}
