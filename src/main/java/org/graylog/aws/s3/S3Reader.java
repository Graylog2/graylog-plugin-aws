package org.graylog.aws.s3;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3Object;
import okhttp3.HttpUrl;
import org.apache.commons.io.IOUtils;
import org.graylog.aws.config.Proxy;
import org.graylog2.plugin.Tools;

import java.io.IOException;

public class S3Reader {

    private final AmazonS3Client client;

    public S3Reader(Region region, HttpUrl proxyUrl, String accessKey, String secretKey) {
        AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);

        if(proxyUrl != null) {
            this.client = new AmazonS3Client(credentials, Proxy.forAWS(proxyUrl));
        } else {
            this.client = new AmazonS3Client(credentials);
        }

        this.client.setRegion(region);
    }

    public String readCompressed(String bucket, String key) throws IOException {
        S3Object o = this.client.getObject(bucket, key);
        
        if (o == null) {
            throw new RuntimeException("Could not get S3 object from bucket [" + bucket + "].");
        }

        byte[] bytes = IOUtils.toByteArray(o.getObjectContent());
        return Tools.decompressGzip(bytes, 10000000);
    }

}
