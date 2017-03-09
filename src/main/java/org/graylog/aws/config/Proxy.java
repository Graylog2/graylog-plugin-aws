package org.graylog.aws.config;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.ClientConfigurationFactory;
import okhttp3.HttpUrl;

import javax.validation.constraints.NotNull;

public class Proxy {

    public static ClientConfiguration forAWS(@NotNull HttpUrl proxyUrl) {
        return new ClientConfigurationFactory().getConfig()
                .withProxyHost(proxyUrl.host())
                .withProxyPort(proxyUrl.port())
                .withProxyUsername(proxyUrl.username())
                .withProxyPassword(proxyUrl.password());
    }

}
