package org.graylog.aws.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@JsonAutoDetect
@JsonIgnoreProperties(ignoreUnknown = true)
@AutoValue
public abstract class AWSPluginConfiguration {

    @JsonProperty("lookups_enabled")
    public abstract boolean lookupsEnabled();

    @JsonProperty("access_key")
    public abstract String accessKey();

    @JsonProperty("secret_key")
    public abstract String secretKey();


    @JsonCreator
    public static AWSPluginConfiguration create(@JsonProperty("lookups_enabled") boolean lookupsEnabled,
                                                @JsonProperty("access_key") String accessKey,
                                                @JsonProperty("secret_key") String secretKey) {
        return builder()
                .lookupsEnabled(lookupsEnabled)
                .accessKey(accessKey)
                .secretKey(secretKey)
                .build();
    }

    public static Builder builder() {
        return new AutoValue_AWSPluginConfiguration.Builder();
    }

    public boolean isComplete() {
        return accessKey() != null && secretKey() != null
                && !accessKey().isEmpty() && !secretKey().isEmpty();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract Builder lookupsEnabled(boolean lookupsEnabled);

        public abstract Builder accessKey(String accessKey);

        public abstract Builder secretKey(String secretKey);

        public abstract AWSPluginConfiguration build();
    }

}
