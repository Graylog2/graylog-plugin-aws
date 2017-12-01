package org.graylog.aws.inputs.guardduty.json;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GuardDutyFindingDetail {

    @JsonProperty
    public String type;

    @JsonProperty
    public String region;

    @JsonProperty
    public int severity;

    public String createdAt;

    @JsonProperty
    public String title;

    @JsonProperty
    public String description;

}
