package org.graylog.aws.s3;

import com.amazonaws.regions.Region;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;
import okhttp3.HttpUrl;
import org.apache.commons.io.IOUtils;
import org.graylog.aws.auth.AWSAuthProvider;
import org.graylog.aws.config.Proxy;
import org.graylog2.plugin.Tools;

import java.io.IOException;

public class S3Reader {

    private final AmazonS3 client;

    public S3Reader(Region region, HttpUrl proxyUrl, AWSAuthProvider authProvider) {
        AmazonS3ClientBuilder clientBuilder = AmazonS3ClientBuilder.standard().withRegion(region.getName()).withCredentials(authProvider);

        if(proxyUrl != null) {
            clientBuilder.withClientConfiguration(Proxy.forAWS(proxyUrl));
        }

        this.client = clientBuilder.build();
    }

    public String readCompressed(String bucket, String key) throws IOException {
        S3Object o = this.client.getObject(bucket, key);

        if (o == null) {
            throw new RuntimeException("Could not get S3 object from bucket [" + bucket + "].");
        }

        byte[] bytes = IOUtils.toByteArray(o.getObjectContent());
        return Tools.decompressGzip(bytes);
    }

}
