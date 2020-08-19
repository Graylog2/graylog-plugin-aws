package org.graylog.aws.inputs.cloudtrail;

import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.journal.RawMessage;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;

import org.junit.Test;

import static org.junit.Assert.*;

public class CloudTrailCodecTest {

    @Test
    public void testAdditionalEventDataField() {

        final CloudTrailCodec codec = new CloudTrailCodec(Configuration.EMPTY_CONFIGURATION,
                new ObjectMapperProvider().get());

        // Decode message with error code
        final RawMessage rawMessage = new RawMessage(("{\n" +
                "\"eventVersion\": \"1.05\",\n" +
                "\"userIdentity\": {\n" +
                "\"type\": \"IAMUser\",\n" +
                "\"principalId\": \"AIDAJHGSCCCCBBBBAAAA\",\n" +
                "\"arn\": \"arn:aws:iam::1111122221111:user/some.user\",\n" +
                "\"accountId\": \"1111122221111\",\n" +
                "\"userName\": \"some.user\"" +
                "},\n" +
                "\"eventTime\": \"2020-08-19T14:12:28Z\",\n" +
                "\"eventSource\": \"signin.amazonaws.com\",\n" +
                "\"eventName\": \"ConsoleLogin\",\n" +
                "\"awsRegion\": \"us-east-1\",\n" +
                "\"sourceIPAddress\": \"127.0.0.1\",\n" +
                "\"userAgent\": \"Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/84.0.4147.125 Safari/537.36\",\n" +
                "\"requestParameters\": null,\n" +
                "\"responseElements\": {\n" +
                "\"ConsoleLogin\": \"Success\"\n" +
                "},\n" +
                "\"additionalEventData\": {\n" +
                "\"LoginTo\": \"https://console.aws.amazon.com/something\",\n" +
                "\"MobileVersion\": \"No\",\n" +
                "\"MFAUsed\": \"Yes\"\n" +
                "},\n" +
                "\"eventID\": \"df38ed44-32d4-43f6-898f-5a55d260a2bb\",\n" +
                "\"eventType\": \"AwsConsoleSignIn\",\n" +
                "\"recipientAccountId\": \"1111122221111\"\n" +
        "}").getBytes());
        Message message = codec.decode(rawMessage);
        String additional_event_data = message.getField("additional_event_data").toString();

        assertTrue(additional_event_data.contains("MFAUsed=Yes"));
        assertTrue(additional_event_data.contains("MobileVersion=No"));
        assertTrue(additional_event_data.contains("LoginTo=https://console.aws.amazon.com/something"));
    }

    @Test
    public void testNoAdditionalEventDataField() {

        final CloudTrailCodec codec = new CloudTrailCodec(Configuration.EMPTY_CONFIGURATION,
                new ObjectMapperProvider().get());

        final RawMessage rawMessage = new RawMessage(("{\n" +
                "\"eventVersion\": \"1.05\",\n" +
                "\"userIdentity\": {\n" +
                "\"type\": \"IAMUser\",\n" +
                "\"principalId\": \"AIDAJHGSCCCCBBBBAAAA\",\n" +
                "\"arn\": \"arn:aws:iam::1111122221111:user/some.user\",\n" +
                "\"accountId\": \"1111122221111\",\n" +
                "\"userName\": \"some.user\"" +
                "},\n" +
                "\"eventTime\": \"2020-08-19T14:12:28Z\",\n" +
                "\"eventSource\": \"signin.amazonaws.com\",\n" +
                "\"eventName\": \"ConsoleLogin\",\n" +
                "\"awsRegion\": \"us-east-1\",\n" +
                "\"sourceIPAddress\": \"127.0.0.1\",\n" +
                "\"userAgent\": \"Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/84.0.4147.125 Safari/537.36\",\n" +
                "\"requestParameters\": null,\n" +
                "\"responseElements\": {\n" +
                "\"ConsoleLogin\": \"Success\"\n" +
                "},\n" +
                "\"eventID\": \"df38ed44-32d4-43f6-898f-5a55d260a2bb\",\n" +
                "\"eventType\": \"AwsConsoleSignIn\",\n" +
                "\"recipientAccountId\": \"1111122221111\"\n" +
        "}").getBytes());
        Message message = codec.decode(rawMessage);
        assertNull(message.getField("additional_event_data"));
    }
}