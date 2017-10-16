package org.graylog.aws.inputs.cloudtrail.messages;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import org.graylog.aws.inputs.cloudtrail.json.CloudTrailRecord;
import org.graylog.aws.inputs.cloudtrail.json.CloudTrailRecordList;

import java.io.IOException;
import java.util.List;

public class TreeReader {
    private final ObjectMapper om;

    public TreeReader(ObjectMapper om) {
        this.om = om;
    }

    public List<CloudTrailRecord> read(String json) throws IOException {
        CloudTrailRecordList tree = om.readValue(json, CloudTrailRecordList.class);
        return ImmutableList.copyOf(tree.records);
    }
}
