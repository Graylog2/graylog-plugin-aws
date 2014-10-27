package com.graylog2.input.cloudtrail.messages;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.graylog2.input.cloudtrail.json.CloudTrailRecord;
import com.graylog2.input.cloudtrail.json.CloudTrailRecordList;
import org.graylog2.plugin.Message;
import org.joda.time.DateTime;

import java.io.IOException;
import java.util.List;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class TreeReader {

    private final ObjectMapper om;

    public TreeReader() {
        om = new ObjectMapper();
    }

    public List<Message> read(String json) throws IOException {
        List<Message> messages = Lists.newArrayList();
        CloudTrailRecordList tree = om.readValue(json, CloudTrailRecordList.class);

        for (CloudTrailRecord record : tree.records) {
            Message message = new Message(
                    record.getConstructedMessage(),
                    "aws-cloudtrail",
                    DateTime.parse(record.eventTime)
            );

            message.addFields(record.additionalFieldsAsMap());
            message.addField("full_message", record.getFullMessage());

            messages.add(message);
        }

        return messages;
    }

}
