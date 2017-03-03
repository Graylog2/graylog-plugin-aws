package org.graylog.aws.s3;

import com.amazonaws.regions.Region;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3Object;
import org.apache.commons.io.IOUtils;
import org.graylog2.plugin.Tools;

import java.io.IOException;

public class S3Reader {

    public String readCompressed(Region region, String bucket, String key) throws IOException {
        AmazonS3Client c = new AmazonS3Client();
        c.setRegion(region);

        S3Object o = c.getObject(bucket, key);

        
        if (o == null) {
            throw new RuntimeException("Could not get S3 object from bucket [" + bucket + "].");
        }

        byte[] bytes = IOUtils.toByteArray(o.getObjectContent());
        return Tools.decompressGzip(bytes, 10000000);
    }

}
