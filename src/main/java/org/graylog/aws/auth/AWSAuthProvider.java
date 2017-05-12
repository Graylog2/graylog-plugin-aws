package org.graylog.aws.auth;


import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import org.graylog.aws.config.AWSPluginConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AWSAuthProvider implements AWSCredentialsProvider {
    private static final Logger LOG = LoggerFactory.getLogger(AWSAuthProvider.class);

    private AWSCredentialsProvider credentials;

    public AWSAuthProvider(AWSPluginConfiguration config, String accessKey, String secretKey) {
        if (accessKey != null && secretKey != null
                && !accessKey.isEmpty() && !secretKey.isEmpty()) {
            this.credentials = new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey));
            LOG.debug("Using input specific config");
        } else if (config.accessKey() != null && config.secretKey() != null
                && !config.accessKey().isEmpty() && !config.secretKey().isEmpty()) {
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
