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

import com.expedia.open.tracing.Span;
import com.expedia.www.haystack.agent.core.Dispatcher;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.util.JsonFormat.Printer;
import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dispatches the data into stdout.
 */
public class LoggerDispatcher implements Dispatcher {
    private final static Logger LOGGER = LoggerFactory.getLogger(LoggerDispatcher.class);

    private static class JSONSerializableSpan {
        Span delegate;
        static Printer printer = JsonFormat.printer().omittingInsignificantWhitespace();

        public JSONSerializableSpan(Span span) {
            delegate = span;
        }

        public String toString() {
            try {
                return printer.print(delegate);
            } catch (InvalidProtocolBufferException e) {
                return "";
            }
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
            LOGGER.debug("{}", new JSONSerializableSpan(span));
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
