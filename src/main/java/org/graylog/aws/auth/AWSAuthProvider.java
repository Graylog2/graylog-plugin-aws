package org.graylog.aws.auth;


import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import org.graylog.aws.config.AWSPluginConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

import static com.google.common.base.Strings.isNullOrEmpty;

public class AWSAuthProvider implements AWSCredentialsProvider {
    private static final Logger LOG = LoggerFactory.getLogger(AWSAuthProvider.class);

    private AWSCredentialsProvider credentials;

    public AWSAuthProvider(AWSPluginConfiguration config) {
        this(config, null, null);
    }

    public AWSAuthProvider(AWSPluginConfiguration config, @Nullable String accessKey, @Nullable String secretKey) {
        if (!isNullOrEmpty(accessKey) && !isNullOrEmpty(secretKey)) {
            this.credentials = new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey));
            LOG.debug("Using input specific config");
        } else if (!isNullOrEmpty(config.accessKey()) && !isNullOrEmpty(config.secretKey())) {
            this.credentials = new AWSStaticCredentialsProvider(new BasicAWSCredentials(config.accessKey(), config.secretKey()));
            LOG.debug("Using AWS Plugin config");
        } else {
            this.credentials = new DefaultAWSCredentialsProviderChain();
            LOG.debug("Using Default Provider Chain");
        }
    }

    @Override
    public AWSCredentials getCredentials() {
        return this.credentials.getCredentials();
    }

    @Override
    public void refresh() {
        this.credentials.refresh();
    }
}
