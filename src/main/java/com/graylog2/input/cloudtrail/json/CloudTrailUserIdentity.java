package com.graylog2.input.cloudtrail.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Maps;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CloudTrailUserIdentity {
    @JsonProperty("type")
    public String type;
    @JsonProperty("principalId")
    public String principalId;
    @JsonProperty("arn")
    public String arn;
    @JsonProperty("accountId")
    public String accountId;
    @JsonProperty("accessKeyId")
    public String accessKeyId;
    @JsonProperty("userName")
    public String userName;
    @JsonProperty("sessionContext")
    public CloudTrailSessionContext sessionContext;

    public Map<String, Object> additionalFieldsAsMap() {
        Map<String, Object> m = Maps.newHashMap();

        m.put("user_type", type);
        m.put("user_name", userName);
        m.put("user_principal_id", principalId);
        m.put("user_principal_arn", arn);
        m.put("user_account_id", accountId);
        m.put("user_access_key_id", accessKeyId);

        if (sessionContext != null && sessionContext.attributes != null) {
            m.put("user_session_creation_date", sessionContext.attributes.creationDate);
            m.put("user_session_mfa_authenticated", Boolean.valueOf(sessionContext.attributes.mfaAuthenticated));
        }

        return m;
    }

}
