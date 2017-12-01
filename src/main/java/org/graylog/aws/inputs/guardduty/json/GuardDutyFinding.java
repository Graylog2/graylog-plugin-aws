package org.graylog.aws.inputs.guardduty.json;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GuardDutyFinding {

    @JsonProperty
    public String source;

    @JsonProperty
    public String account;

    @JsonProperty
    public GuardDutyFindingDetail detail;

}
