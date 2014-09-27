package com.graylog2.input.cloudtrail.json;

import java.util.List;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class CloudtrailWriteNotification {

    public String s3Bucket;
    public List<String> s3ObjectKey;

}
