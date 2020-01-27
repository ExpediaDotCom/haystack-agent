/*
 *  Copyright 2017 Expedia, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.expedia.www.haystack.agent.dispatcher;

import com.expedia.open.tracing.Log;
import com.expedia.open.tracing.Span;
import com.expedia.open.tracing.Tag;
import com.expedia.www.haystack.agent.core.Dispatcher;
import com.google.protobuf.InvalidProtocolBufferException;
import com.typesafe.config.Config;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dispatches the data into stdout.
 */
public class LoggerDispatcher implements Dispatcher {
    private final static Logger LOGGER = LoggerFactory.getLogger(LoggerDispatcher.class);

    private static class JSONSpan {
        Span delegate;

        public JSONSpan(Span span) {
            delegate = span;
        }

        public String toString() {
            StringBuilder serialized = new StringBuilder("{"
                    + "\"traceId\":\"" + delegate.getTraceId() + "\","
                    + "\"spanId\":\"" + delegate.getSpanId() + "\",");

            if (!delegate.getParentSpanId().isEmpty()) {
                serialized.append("\"parentSpanId\":\"").append(delegate.getParentSpanId()).append("\",");
            }

            if (!delegate.getServiceName().isEmpty()) {
                serialized.append("\"serviceName\":\"").append(delegate.getServiceName()).append("\",");
            }

            if (!delegate.getServiceName().isEmpty()) {
                serialized.append("\"operationName\":\"").append(delegate.getOperationName()).append("\",");
            }

            serialized.append("\"startTime\":").append(delegate.getStartTime());

            if (delegate.getDuration() > 0) {
                serialized.append("\"duration\":").append(delegate.getDuration());
            }

            int i;
            int logsCount = delegate.getLogsCount();
            if (logsCount > 0) {
                serialized.append(",\"logs\":[");
                for (i = 0; i < logsCount; i++) {
                    Log log = delegate.getLogs(i);
                    serialized.append("{\"timestamp\":").append(log.getTimestamp());
                    if (log.getFieldsCount() > 0) {
                        serialized.append(",");
                        serializeTags(serialized, "fields", log.getFieldsList());
                    }
                    serialized.append("}");
                }
                serialized.append("]");
            }

            if (delegate.getTagsCount() > 0) {
                serialized.append(",");
                serializeTags(serialized, "tags", delegate.getTagsList());
            }

            serialized.append("}");
            return serialized.toString();
        }

        private static void serializeTags(StringBuilder serialized, String fieldName, List<Tag> tags) {
            if (tags.isEmpty()) {
                return;
            }

            int i;
            serialized.append("\"").append(fieldName).append("\":[");
            for (i = 0; i < tags.size(); i++) {
                if (i > 0) {
                    serialized.append(",");
                }
                Tag tag = tags.get(i);
                serialized.append("{\"key\":\"").append(tag.getKey()).append("\",");
                serialized.append("\"type\":\"").append(tag.getType()).append("\",");
                switch (tag.getType()) {
                    case STRING:
                        serialized.append("\"vString\":\"").append(tag.getVStr()).append("\"}");
                        break;
                    case DOUBLE:
                        serialized.append("\"vDouble\":").append(tag.getVDouble()).append("}");
                        break;
                    case BOOL:
                        serialized.append("\"vBool\":").append(tag.getVBool()).append("}");
                        break;
                    case LONG:
                        serialized.append("\"vLong\":").append(tag.getVLong()).append("}");
                        break;
                    case BINARY:
                        serialized.append("\"vBinary\":\"").append(tag.getVBytes()).append("\"}");
                        break;
                }
            }
            serialized.append("]");
        }
    }

    @Override
    public String getName() {
        return "logger";
    }

    @Override
    public void dispatch(final byte[] partitionKey, final byte[] data) {
        try {
            Span span = Span.parseFrom(data);
            LOGGER.debug("{}", new JSONSpan(span));
        } catch (InvalidProtocolBufferException ex) {
            LOGGER.error("failed to parse span: " + ex);
        }
    }

    @Override
    public void initialize(final Config conf) {
    }

    @Override
    public void close() {
    }
}
