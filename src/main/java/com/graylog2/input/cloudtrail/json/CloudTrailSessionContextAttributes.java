package com.graylog2.input.cloudtrail.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CloudTrailSessionContextAttributes {

    public String mfaAuthenticated;
    public String creationDate;

}
