package com.graylog2.input.cloudtrail.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CloudTrailRecordList {
    @JsonProperty("Records")
    public List<CloudTrailRecord> records;

}
