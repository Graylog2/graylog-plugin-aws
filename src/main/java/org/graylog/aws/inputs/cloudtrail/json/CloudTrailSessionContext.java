package org.graylog.aws.inputs.cloudtrail.json;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CloudTrailSessionContext {
    @JsonProperty("attributes")
    public CloudTrailSessionContextAttributes attributes;

}
