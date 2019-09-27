package org.graylog.aws.inputs.cloudtrail;

import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.journal.RawMessage;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Assert;
import org.junit.Test;

public class CloudTrailCodecTest {

    @Test
    public void testCodec() {

        final CloudTrailCodec codec = new CloudTrailCodec(Configuration.EMPTY_CONFIGURATION,
                                                          new ObjectMapperProvider().get());

        // Decode message with error code
        final RawMessage rawMessage = new RawMessage(("{\n" +
                                                      "    \"eventVersion\": \"1.0\",\n" +
                                                      "    \"userIdentity\": {\n" +
                                                      "        \"type\": \"IAMUser\",\n" +
                                                      "        \"principalId\": \"EX_PRINCIPAL_ID\",\n" +
                                                      "        \"arn\": \"arn:aws:iam::123456789012:user/Alice\",\n" +
                                                      "        \"accountId\": \"123456789012\",\n" +
                                                      "        \"accessKeyId\": \"EXAMPLE_KEY_ID\",\n" +
                                                      "        \"userName\": \"Alice\"\n" +
                                                      "    },\n" +
                                                      "    \"eventTime\": \"2014-03-24T21:11:59Z\",\n" +
                                                      "    \"eventSource\": \"iam.amazonaws.com\",\n" +
                                                      "    \"eventName\": \"CreateUser\",\n" +
                                                      "    \"awsRegion\": \"us-east-2\",\n" +
                                                      "    \"sourceIPAddress\": \"127.0.0.1\",\n" +
                                                      "    \"userAgent\": \"aws-cli/1.3.2 Python/2.7.5 Windows/7\",\n" +
                                                      "    \"requestParameters\": {\"userName\": \"Bob\"},\n" +
                                                      "    \"responseElements\": {\"user\": {\n" +
                                                      "        \"createDate\": \"Mar 24, 2014 9:11:59 PM\",\n" +
                                                      "        \"userName\": \"Bob\",\n" +
                                                      "        \"arn\": \"arn:aws:iam::123456789012:user/Bob\",\n" +
                                                      "        \"path\": \"/\",\n" +
                                                      "        \"userId\": \"EXAMPLEUSERID\"\n" +
                                                      "    }}\n" +
                                                      "}").getBytes());
        final Message message = codec.decode(rawMessage);
        // TODO: Some assertions to verify message contents (and error code)

        final RawMessage noErrorRawMessage = new RawMessage(("{\n" +
                                                          "  \"eventVersion\": \"1.04\",\n" +
                                                          "  \"userIdentity\": {\n" +
                                                          "    \"type\": \"IAMUser\",\n" +
                                                          "    \"principalId\": \"EX_PRINCIPAL_ID\",\n" +
                                                          "    \"arn\": \"arn:aws:iam::123456789012:user/Alice\",\n" +
                                                          "    \"accountId\": \"123456789012\",\n" +
                                                          "    \"accessKeyId\": \"EXAMPLE_KEY_ID\",\n" +
                                                          "    \"userName\": \"Alice\"\n" +
                                                          "  },\n" +
                                                          "  \"eventTime\": \"2016-07-14T19:15:45Z\",\n" +
                                                          "  \"eventSource\": \"cloudtrail.amazonaws.com\",\n" +
                                                          "  \"eventName\": \"UpdateTrail\",\n" +
                                                          "  \"awsRegion\": \"us-east-2\",\n" +
                                                          "  \"sourceIPAddress\": \"205.251.233.182\",\n" +
                                                          "  \"userAgent\": \"aws-cli/1.10.32 Python/2.7.9 Windows/7 botocore/1.4.22\",\n" +
                                                          "  \"requestParameters\": {\n" +
                                                          "    \"name\": \"myTrail2\"\n" +
                                                          "  },\n" +
                                                          "  \"responseElements\": null,\n" +
                                                          "  \"requestID\": \"5d40662a-49f7-11e6-97e4-d9cb6ff7d6a3\",\n" +
                                                          "  \"eventID\": \"b7d4398e-b2f0-4faa-9c76-e2d316a8d67f\",\n" +
                                                          "  \"eventType\": \"AwsApiCall\",\n" +
                                                          "  \"recipientAccountId\": \"123456789012\"\n" +
                                                          "}").getBytes());
        final Message noErrorMessage = codec.decode(noErrorRawMessage);
        // TODO: Some assertions to verify message contents
    }
}
