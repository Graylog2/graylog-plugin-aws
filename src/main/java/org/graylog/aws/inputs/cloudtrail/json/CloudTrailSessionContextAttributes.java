package org.graylog.aws.inputs.cloudtrail.json;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CloudTrailSessionContextAttributes {
    @JsonProperty("mfaAuthenticated")
    public String mfaAuthenticated;
    @JsonProperty("creationDate")
    public String creationDate;

}
