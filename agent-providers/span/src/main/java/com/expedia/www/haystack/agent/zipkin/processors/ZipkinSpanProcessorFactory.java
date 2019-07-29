/*
 *  Copyright 2019 Expedia, Inc.
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

package com.expedia.www.haystack.agent.zipkin.processors;

import com.expedia.www.haystack.agent.core.Dispatcher;
import com.expedia.www.haystack.agent.span.enricher.Enricher;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.Validate;
import zipkin2.codec.SpanBytesDecoder;

import java.util.List;
import java.util.Map;

public class ZipkinSpanProcessorFactory {
    public static String JSON_CONTENT_TYPE = "application/json";
    public static String THRIFT_CONTENT_TYPE = "application/x-thrift";
    public static String PROTO_CONTENT_TYPE = "application/x-protobuf";

    private final List<Dispatcher> dispatchers;
    private final SpanValidator validator;
    private final List<Enricher> enrichers;

    public ZipkinSpanProcessorFactory(final SpanValidator validator,
                                      final List<Dispatcher> dispatchers,
                                      final List<Enricher> enrichers) {

        Validate.notNull(validator, "span validator can't be null");
        Validate.notEmpty(dispatchers, "dispatchers can't be null or empty");
        Validate.notNull(enrichers, "enrichers can't be null or empty");

        this.validator = validator;
        this.dispatchers = dispatchers;
        this.enrichers = enrichers;
    }

    public Map<String, ZipkinSpanProcessor> v1() {
        return ImmutableMap.of(
                JSON_CONTENT_TYPE, new ZipkinSpanProcessor(SpanBytesDecoder.JSON_V1, validator, dispatchers, enrichers),
                THRIFT_CONTENT_TYPE, new ZipkinSpanProcessor(SpanBytesDecoder.THRIFT, validator, dispatchers, enrichers));
    }

    public Map<String, ZipkinSpanProcessor> v2() {
        return ImmutableMap.of(
                JSON_CONTENT_TYPE, new ZipkinSpanProcessor(SpanBytesDecoder.JSON_V2, validator, dispatchers, enrichers),
                PROTO_CONTENT_TYPE, new ZipkinSpanProcessor(SpanBytesDecoder.PROTO3, validator, dispatchers, enrichers));
    }
}
