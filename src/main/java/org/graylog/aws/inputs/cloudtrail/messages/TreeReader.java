package org.graylog.aws.inputs.cloudtrail.messages;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.graylog.aws.inputs.cloudtrail.json.CloudTrailRecord;
import org.graylog.aws.inputs.cloudtrail.json.CloudTrailRecordList;

import java.io.IOException;
import java.util.List;

public class TreeReader {
    private final ObjectMapper om;

    public TreeReader() {
        om = new ObjectMapper();
    }

    public List<CloudTrailRecord> read(String json) throws IOException {
        List<CloudTrailRecord> messages = Lists.newArrayList();
        CloudTrailRecordList tree = om.readValue(json, CloudTrailRecordList.class);

        for (CloudTrailRecord record : tree.records) {
            messages.add(record);
        }

        return messages;
    }

}
