package com.graylog2.input.s3;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3Object;
import com.graylog2.input.AWSInput;
import org.apache.commons.io.IOUtils;
import org.graylog2.plugin.Tools;

import java.io.IOException;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class S3Reader {

    public static String readCompressed(String bucket, String key) throws IOException {
        AWSCredentials credentials = new BasicAWSCredentials(AWSInput.ACCESS_KEY, AWSInput.SECRET_KEY);
        AmazonS3Client c = new AmazonS3Client(credentials);
        S3Object o = c.getObject(bucket, key);

        if (o == null) {
            throw new RuntimeException("Could not get S3 object from bucket [" + bucket + "].");
        }

        byte[] bytes = IOUtils.toByteArray(o.getObjectContent());
        return Tools.decompressGzip(bytes);
    }

}
