package org.graylog.aws.inputs.cloudtrail.notifications;

import com.amazonaws.services.sqs.model.Message;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;

public class CloudtrailSNSNotificationParserTest {

    public static final Message MESSAGE = new Message()
            .withBody("{\n" +
                    "  \"Type\" : \"Notification\",\n" +
                    "  \"MessageId\" : \"55508fe9-870b-590c-960f-c34960b669f0\",\n" +
                    "  \"TopicArn\" : \"arn:aws:sns:eu-west-1:459220251735:cloudtrail-write\",\n" +
                    "  \"Message\" : \"{\\\"s3Bucket\\\":\\\"cloudtrailbucket\\\",\\\"s3ObjectKey\\\":[\\\"example/AWSLogs/459220251735/CloudTrail/eu-west-1/2014/09/27/459220251735_CloudTrail_eu-west-1_20140927T1625Z_UPwzr7ft2mf0Q1SS.json.gz\\\"]}\",\n" +
                    "  \"Timestamp\" : \"2014-09-27T16:27:41.258Z\",\n" +
                    "  \"SignatureVersion\" : \"1\",\n" +
                    "  \"Signature\" : \"O05joR97NvGHqMJQwsSNXzeSHrtbLqbRcqsXB7xmqARyaCGXjaVh2duwTUL93s4YvoNENnOEMzkILKI5PwmQQPha5/cmj6FSjblwRMMga6Xzf6cMnurT9TphQO7z35foHG49IejW05IkzIwD/DW0GvafJLah+fQI3EFySnShzXLFESGQuumdS8bxnM5r96ne8t+MEAHfBCVyQ/QrduO9tTtfXAz6OeWg1IEwV3TeZ5c5SS5vRxxhsD4hOJSmXAUM99CeQfcG9s7saBcvyyGPZrhPEh8S1uhiTmLvr6h1voM9vgiCbCCUujExvg+bnqsXWTZBmnatF1iOyxFfYcZ6kw==\",\n" +
                    "  \"SigningCertURL\" : \"https://sns.eu-west-1.amazonaws.com/SimpleNotificationService-d6d679a1d18e95c2f9ffcf11f4f9e198.pem\",\n" +
                    "  \"UnsubscribeURL\" : \"https://sns.eu-west-1.amazonaws.com/?Action=Unsubscribe&SubscriptionArn=arn:aws:sns:eu-west-1:459220251735:cloudtrail-write:9a3a4e76-4173-4c8c-b488-0126315ba643\"\n" +
                    "}");

    public static final Message DOUBLE_MESSAGE = new Message()
            .withBody("{\n" +
                    "  \"Type\" : \"Notification\",\n" +
                    "  \"MessageId\" : \"11a04c4a-094e-5395-b297-00eaefda2893\",\n" +
                    "  \"TopicArn\" : \"arn:aws:sns:eu-west-1:459220251735:cloudtrail-write\",\n" +
                    "  \"Message\" : \"{\\\"s3Bucket\\\":\\\"cloudtrailbucket\\\",\\\"s3ObjectKey\\\":[\\\"example/AWSLogs/459220251735/CloudTrail/eu-west-1/2014/09/27/459220251735_CloudTrail_eu-west-1_20140927T1620Z_Nk2SdmlEzA0gDpPr.json.gz\\\", \\\"example/AWSLogs/459220251735/CloudTrail/eu-west-1/2014/09/27/459220251999_CloudTrail2_eu-west-1_20140927T1620Z_Nk2SdmlEzA0gDpPr.json.gz\\\"]}\",\n" +
                    "  \"Timestamp\" : \"2014-09-27T16:22:44.011Z\",\n" +
                    "  \"SignatureVersion\" : \"1\",\n" +
                    "  \"Signature\" : \"q9xmJZ8nJR5iaAYMLN3M8v9HyLbUqbLjGGFlmmvIK9UDQiQO0wmvlYeo5/lQqvANW/v+NVXZxxOoWx06p6Rv5BwXIa2ASVh7RlXc2y+U2pQgLaQlJ671cA33iBi/iH1al/7lTLrlIkUb9m2gAdEyulbhZfBfAQOm7GN1PHR/nW+CtT61g4KvMSonNzj23jglLTb0r6pxxQ5EmXz6Jo5DOsbXvuFt0BSyVP/8xRXT1ap0S7BuUOstz8+FMqdUyOQSR9RA9r61yUsJ4nnq0KfK5/1gjTTDPmE4OkGvk6AuV9YTME7FWTY/wU4LPg5/+g/rUo2UDGrxnGoJ3OUW5yrtyQ==\",\n" +
                    "  \"SigningCertURL\" : \"https://sns.eu-west-1.amazonaws.com/SimpleNotificationService-d6d679a1d18e95c2f9ffcf11f4f9e198.pem\",\n" +
                    "  \"UnsubscribeURL\" : \"https://sns.eu-west-1.amazonaws.com/?Action=Unsubscribe&SubscriptionArn=arn:aws:sns:eu-west-1:459220251735:cloudtrail-write:9a3a4e76-4173-4c8c-b488-0126315ba643\"\n" +
                    "}");

    @Test
    public void testParse() throws Exception {
        CloudtrailSNSNotificationParser parser = new CloudtrailSNSNotificationParser();

        List<CloudtrailSNSNotification> notifications = parser.parse(MESSAGE);
        assertEquals(1, notifications.size());

        CloudtrailSNSNotification notification = notifications.get(0);

        assertEquals(notification.getS3Bucket(), "cloudtrailbucket");
        assertEquals(notification.getS3ObjectKey(), "example/AWSLogs/459220251735/CloudTrail/eu-west-1/2014/09/27/459220251735_CloudTrail_eu-west-1_20140927T1625Z_UPwzr7ft2mf0Q1SS.json.gz");
    }

    @Test
    public void testParseWithTwoS3Objects() throws Exception {
        CloudtrailSNSNotificationParser parser = new CloudtrailSNSNotificationParser();

        List<CloudtrailSNSNotification> notifications = parser.parse(DOUBLE_MESSAGE);
        assertEquals(2, notifications.size());

        CloudtrailSNSNotification notification1 = notifications.get(0);
        CloudtrailSNSNotification notification2 = notifications.get(1);

        assertEquals(notification1.getS3Bucket(), "cloudtrailbucket");
        assertEquals(notification1.getS3ObjectKey(), "example/AWSLogs/459220251735/CloudTrail/eu-west-1/2014/09/27/459220251735_CloudTrail_eu-west-1_20140927T1620Z_Nk2SdmlEzA0gDpPr.json.gz");

        assertEquals(notification2.getS3Bucket(), "cloudtrailbucket");
        assertEquals(notification2.getS3ObjectKey(), "example/AWSLogs/459220251735/CloudTrail/eu-west-1/2014/09/27/459220251999_CloudTrail2_eu-west-1_20140927T1620Z_Nk2SdmlEzA0gDpPr.json.gz");
    }
}
