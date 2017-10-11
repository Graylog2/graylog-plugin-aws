package org.graylog.aws.inputs.cloudtrail.json;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class CloudTrailRecordList {
    @JsonProperty("Records")
    public List<CloudTrailRecord> records;
}
