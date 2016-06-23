package org.graylog.aws.processors.instancelookup;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@JsonAutoDetect
@JsonIgnoreProperties(ignoreUnknown = true)
@AutoValue
public abstract class AWSInstanceNameLookupConfig {

    @JsonProperty("enabled")
    public abstract boolean enabled();

    @JsonProperty("access_key")
    public abstract String accessKey();

    @JsonProperty("secret_key")
    public abstract String secretKey();

    @JsonCreator
    public static AWSInstanceNameLookupConfig create(@JsonProperty("enabled") boolean enabled,
                                                     @JsonProperty("access_key") String accessKey,
                                                     @JsonProperty("secret_ket") String secretKey) {
        return builder()
                .enabled(enabled)
                .accessKey(accessKey)
                .secretKey(secretKey)
                .build();
    }

    public static Builder builder() {
        return new AutoValue_AWSInstanceNameLookupConfig.Builder();
    }

    public boolean isComplete() {
        return accessKey() != null && secretKey() != null
                && !accessKey().isEmpty() && !secretKey().isEmpty();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract Builder enabled(boolean enabled);

        public abstract Builder accessKey(String accessKey);

        public abstract Builder secretKey(String secretKey);

        public abstract AWSInstanceNameLookupConfig build();
    }

}
