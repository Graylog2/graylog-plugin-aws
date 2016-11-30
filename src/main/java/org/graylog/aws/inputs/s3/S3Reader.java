package org.graylog.aws.inputs.s3;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3Object;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

public class S3Reader {

    private final AmazonS3Client s3Client;

    public S3Reader(String accessKey, String secretKey) {
        this.s3Client = new AmazonS3Client(new BasicAWSCredentials(accessKey, secretKey));
    }

    public InputStream readStream(Region region, String bucket, String key) throws IOException {
        S3Object o = getS3Object(region, bucket, key);

        return o.getObjectContent();
    }

    public InputStream readCompressedStream(Region region, String bucket, String key) throws IOException {
        S3Object o = getS3Object(region, bucket, key);

        return new GZIPInputStream(o.getObjectContent());
    }

    private S3Object getS3Object(Region region, String bucket, String key) {
        s3Client.setRegion(region);

        S3Object o = s3Client.getObject(bucket, key);

        if (o == null) {
            throw new RuntimeException("Could not get S3 object from bucket [" + bucket + "].");
        }
        return o;
    }

}