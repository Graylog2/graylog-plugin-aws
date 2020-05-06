package org.graylog.aws.config;

import com.amazonaws.regions.Regions;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.aws.config.AWSPluginConfiguration.createDefault;

public class AWSPluginConfigurationTest {
    @Test
    public void lookupRegions() throws Exception {
        final AWSPluginConfiguration config = createDefault()
                .toBuilder()
                .lookupRegions("us-west-1,eu-west-1 ,  us-east-1 ")
                .build();

        assertThat(config.getLookupRegions()).containsExactly(Regions.US_WEST_1, Regions.EU_WEST_1, Regions.US_EAST_1);
    }

    @Test
    public void lookupRegionsWithEmptyValue() throws Exception {
        final AWSPluginConfiguration config = createDefault()
                .toBuilder()
                .lookupRegions("")
                .build();

        assertThat(config.getLookupRegions()).isEmpty();
    }

    @Test
    public void encryptsSecretKeyAndGeneratesSalt() {
        final AWSPluginConfiguration config = createDefault().toBuilder()
                .secretKey("verysecret", "myencryptionKey!")
                .build();

        assertThat(config.encryptedSecretKey()).isNotEqualTo("verysecret");
        assertThat(config.secretKeySalt()).isNotBlank();
        assertThat(config.secretKey("myencryptionKey!")).isEqualTo("verysecret");
    }

    @Test
    public void builderDoesNotTamperWithEncryptedSecretKey() {
        final AWSPluginConfiguration config = createDefault().toBuilder()
                .secretKey("verysecret", "myencryptionKey!")
                .build();

        final AWSPluginConfiguration newConfig = config.toBuilder()
                .accessKey("somethingElse")
                .build();

        assertThat(newConfig.secretKey("myencryptionKey!")).isEqualTo("verysecret");
    }

    @Test
    public void returnsEmptyStringIfEncryptedSecretKeyIsNotSet() {
        assertThat(createDefault().secretKey("foo")).isNull();
    }
}
