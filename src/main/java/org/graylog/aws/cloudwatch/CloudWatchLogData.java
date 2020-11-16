/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.aws.cloudwatch;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * A collection of CloudWatch log events.
 * <p/>
 * Example payload:
 * <pre>
 * {
 *   "messageType": "DATA_MESSAGE",
 *   "owner": "123456789",
 *   "logGroup": "aws-plugin-test-flows",
 *   "logStream": "eni-aaaaaaaa-all",
 *   "subscriptionFilters": ["match-all"],
 *   "logEvents": [
 *     {
 *       "id": "33503748002479370955346306650196094071913271643270021120",
 *       "timestamp": 1502360020000,
 *       "message": "2 123456789 eni-aaaaaaaa 10.0.27.226 10.42.96.199 3604 17720 17 1 132 1502360020 1502360079 REJECT OK"
 *     },
 *     {
 *       "id": "33503748002479370955346306650196094071913271643270021127",
 *       "timestamp": 1502360020000,
 *       "message": "2 123456789 eni-aaaaaaaa 10.0.34.113 10.42.96.199 53421 17720 6 1 48 1502360020 1502360079 REJECT OK"
 *     }
 *   ]
 * }
 * </pre>
 */
public class CloudWatchLogData {

    @JsonProperty("logEvents")
    public List<CloudWatchLogEvent> logEvents;

    @JsonProperty("logGroup")
    public String logGroup;

    @JsonProperty("logStream")
    public String logStream;
}
