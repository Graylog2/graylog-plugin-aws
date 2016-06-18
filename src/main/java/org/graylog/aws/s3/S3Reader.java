package org.graylog.aws.s3;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3Object;
import org.apache.commons.io.IOUtils;
import org.graylog2.plugin.Tools;

import java.io.IOException;

public class S3Reader {

    private final String accessKey;
    private final String secretKey;

    public S3Reader(String accessKey, String secretKey) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

    public String readCompressed(Region region, String bucket, String key) throws IOException {
        AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
        AmazonS3Client c = new AmazonS3Client(credentials);
        c.setRegion(region);

        S3Object o = c.getObject(bucket, key);

        if (o == null) {
            throw new RuntimeException("Could not get S3 object from bucket [" + bucket + "].");
        }

        byte[] bytes = IOUtils.toByteArray(o.getObjectContent());
        return Tools.decompressGzip(bytes);
    }

}
