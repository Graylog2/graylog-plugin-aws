package org.graylog.aws.config;

import com.amazonaws.regions.Regions;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

@JsonAutoDetect
@JsonIgnoreProperties(ignoreUnknown = true)
@AutoValue
public abstract class AWSPluginConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(AWSPluginConfiguration.class);

    @JsonProperty("lookups_enabled")
    public abstract boolean lookupsEnabled();

    @JsonProperty("lookup_regions")
    public abstract String lookupRegions();

    @JsonProperty("access_key")
    public abstract String accessKey();

    @JsonProperty("secret_key")
    public abstract String secretKey();

    @JsonProperty("proxy_enabled")
    public abstract boolean proxyEnabled();

    @JsonCreator
    public static AWSPluginConfiguration create(@JsonProperty("lookups_enabled") boolean lookupsEnabled,
                                                @JsonProperty("lookup_regions") String lookupRegions,
                                                @JsonProperty("access_key") String accessKey,
                                                @JsonProperty("secret_key") String secretKey,
                                                @JsonProperty("proxy_enabled") boolean proxyEnabled) {
        return builder()
                .lookupsEnabled(lookupsEnabled)
                .lookupRegions(lookupRegions)
                .accessKey(accessKey)
                .secretKey(secretKey)
                .proxyEnabled(proxyEnabled)
                .build();
    }

    public static AWSPluginConfiguration createDefault() {
        return builder()
                .lookupsEnabled(false)
                .lookupRegions("")
                .accessKey("")
                .secretKey("")
                .proxyEnabled(false)
                .build();
    }

    public static Builder builder() {
        return new AutoValue_AWSPluginConfiguration.Builder();
    }

    @JsonIgnore
    public List<Regions> getLookupRegions() {
        if (lookupRegions() == null || lookupRegions().isEmpty()) {
            return Collections.emptyList();
        }

        ImmutableList.Builder<Regions> builder = ImmutableList.<Regions>builder();

        String[] regions = lookupRegions().split(",");
        for (String regionName : regions) {
            try {
                builder.add(Regions.fromName(regionName.trim()));
            } catch(IllegalArgumentException e) {
                LOG.info("Cannot translate [{}] into AWS region. Make sure it is a correct region code like for example 'us-west-1'.", regionName);
            }
        }

        return builder.build();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract Builder lookupsEnabled(boolean lookupsEnabled);

        public abstract Builder lookupRegions(String lookupRegions);

        public abstract Builder accessKey(String accessKey);

        public abstract Builder secretKey(String secretKey);

        public abstract Builder proxyEnabled(boolean proxyEnabled);

        public abstract AWSPluginConfiguration build();
    }

}
