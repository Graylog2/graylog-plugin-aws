package org.graylog.aws.inputs.guardduty;

import org.graylog.aws.inputs.guardduty.json.GuardDutyFinding;

import java.util.Map;

public class GuardDutyEventParser {

    // fall back to simple parsing in case of error or unexpected structure. log a warning.
    
    public Map<String, Object> parse(GuardDutyFinding finding) {
        return null;
    }

}
