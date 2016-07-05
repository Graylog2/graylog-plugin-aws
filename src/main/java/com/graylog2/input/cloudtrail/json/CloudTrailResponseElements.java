package com.graylog2.input.cloudtrail.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Maps;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CloudTrailResponseElements{

/*
 * Remark:
 * - All JSON properties are optional.
 * - This list is incomplete.
*/  
   
    @JsonProperty("RenewRole")
    public String renewRole;

    @JsonProperty("ExitRole")
    public String exitRole;

    @JsonProperty("volumeId")
    public String volumeId;

    @JsonProperty("instanceId")
    public String instanceId;

    @JsonProperty("device")
    public String device;

    @JsonProperty("ConsoleLogin")
    public String consoleLogin;

    @JsonProperty("status")
    public String status;

    @JsonProperty("_return")
    public String returnValue;

    @JsonProperty("description")
    public String description;

    

    public Map<String, Object> additionalFieldsAsMap() {
        Map<String, Object> m = Maps.newHashMap();


       if (renewRole != null) {
           m.put("renewRole", renewRole);
        }

       if (exitRole != null) {
           m.put("exitRole", exitRole);
        }
       if (volumeId != null) {
           m.put("volumeId", volumeId);
        }
       if (instanceId != null) {
           m.put("instanceId", instanceId);
        }
       if (device != null) {
           m.put("device", device);
        }
       if (consoleLogin != null) {
           m.put("consoleLogin", consoleLogin);
        }
       if (status != null) {
           m.put("status", status);
        }
       if (returnValue != null) {
           m.put("returnValue", returnValue);
        }

        return m;
    }

}
