package org.graylog.aws.auth;

import org.graylog.aws.config.AWSPluginConfiguration;
import org.graylog2.Configuration;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class AWSAuthProviderTest {
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private Configuration systemConfiguration;

    @Test
    public void encryptSecretKeyFromPluginConfigUsingSystemSecret() {
        when(systemConfiguration.getPasswordSecret()).thenReturn("encryptionKey123");
        final AWSPluginConfiguration config = AWSPluginConfiguration.createDefault()
                .toBuilder()
                .accessKey("MyAccessKey")
                .secretKey("aVerySecretKey", "encryptionKey123")
                .build();

        AWSAuthProvider authProvider = createForConfig(config);

        assertThat(authProvider.getCredentials().getAWSSecretKey()).isEqualTo("aVerySecretKey");
        assertThat(authProvider.getCredentials().getAWSAccessKeyId()).isEqualTo("MyAccessKey");
    }

    private AWSAuthProvider createForConfig(AWSPluginConfiguration config) {
        return new AWSAuthProvider(systemConfiguration, config);

    }
}
