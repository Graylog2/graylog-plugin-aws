package org.graylog.aws.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.base.Strings;

import javax.annotation.Nullable;
import java.util.Optional;

@AutoValue
@JsonAutoDetect
public abstract class AWSPluginConfigurationUpdate {
    abstract boolean lookupsEnabled();

    abstract String lookupRegions();

    abstract String accessKey();

    abstract Optional<String> secretKey();

    abstract boolean proxyEnabled();

    @JsonCreator
    public static AWSPluginConfigurationUpdate create(
            @JsonProperty("lookups_enabled") boolean lookupsEnabled,
            @JsonProperty("lookup_regions") String lookupRegions,
            @JsonProperty("access_key") String accessKey,
            @JsonProperty("secret_key") @Nullable String secretKey,
            @JsonProperty("proxy_enabled") boolean proxyEnabled
    ) {
        return new AutoValue_AWSPluginConfigurationUpdate(
                lookupsEnabled,
                lookupRegions,
                accessKey,
                Optional.ofNullable(Strings.emptyToNull(secretKey)),
                proxyEnabled
        );
    }
}
