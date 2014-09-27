package com.graylog2.input.cloudtrail;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class CloudtrailSQSSubscriber {

    // TODO
    public static final String ACCESS_KEY = "AKIAIFFKDVDZOL2NXCCA";
    public static final String SECRET_KEY = "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";

    private final AmazonSQS sqs;
    private final String queueName;

    public CloudtrailSQSSubscriber(String queueName) {
        AWSCredentials credentials = new BasicAWSCredentials(ACCESS_KEY, SECRET_KEY);

        this.sqs = new AmazonSQSClient(credentials);
        this.sqs.setRegion(Region.getRegion(Regions.EU_WEST_1)); // TODO

        this.queueName = queueName;
    }


    public void getMessages() {
        ReceiveMessageResult result = sqs.receiveMessage(new ReceiveMessageRequest(queueName));

        int i = 0;
        for (Message message : result.getMessages()) {
            System.out.println("#" + i);
            System.out.println("ID  : " + message.getMessageId());
            System.out.println("BODY: " + message.getBody());
            System.out.println("----------------");
            i++;
        }
    }

}
