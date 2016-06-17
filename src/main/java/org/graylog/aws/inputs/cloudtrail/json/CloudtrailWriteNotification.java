package org.graylog.aws.inputs.cloudtrail.json;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class CloudtrailWriteNotification {
    @JsonProperty("s3Bucket")
    public String s3Bucket;
    @JsonProperty("s3ObjectKey")
    public List<String> s3ObjectKey;

}
