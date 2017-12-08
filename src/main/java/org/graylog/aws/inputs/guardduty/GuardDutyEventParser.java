package org.graylog.aws.inputs.guardduty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import org.graylog.aws.inputs.guardduty.json.GuardDutyFinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class GuardDutyEventParser {

    private static final Logger LOG = LoggerFactory.getLogger(GuardDutyEventParser.class);

    private final ObjectMapper om;

    public GuardDutyEventParser(ObjectMapper objectMapper) {
        this.om = objectMapper;
    }

    public Map<String, Object> parse(GuardDutyFinding finding) {
        Map<String, Object> fields = Maps.newHashMap();

        fields.put("account_id", finding.account);
        fields.put("region", finding.detail.region);
        fields.put("severity", finding.detail.severity);
        fields.put("description", finding.detail.description);

        LOG.info(finding.detail.type);

        return fields;
    }

}
