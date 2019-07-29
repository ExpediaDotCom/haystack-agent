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

import com.codahale.metrics.Meter;
import com.expedia.open.tracing.Span;
import com.expedia.www.haystack.agent.core.Dispatcher;
import com.expedia.www.haystack.agent.core.metrics.SharedMetricRegistry;
import com.expedia.www.haystack.agent.span.enricher.Enricher;
import zipkin2.codec.SpanBytesDecoder;

import java.util.List;

public class ZipkinSpanProcessor {
    private final Meter invalidSpanMeter;
    private final SpanBytesDecoder decoder;
    private final SpanValidator validator;
    private final List<Dispatcher> dispatchers;
    private final List<Enricher> enrichers;

    public ZipkinSpanProcessor(final SpanBytesDecoder decoder,
                               final SpanValidator validator,
                               final List<Dispatcher> dispatchers,
                               final List<Enricher> enrichers) {
        this.decoder = decoder;
        this.validator = validator;
        this.dispatchers = dispatchers;
        this.enrichers = enrichers;
        this.invalidSpanMeter = SharedMetricRegistry.newMeter("pitchfork.invalid.spans");
    }

    public void process(byte[] data) throws Exception {
        final List<zipkin2.Span> zipkinSpans = decoder.decodeList(data);
        for (final zipkin2.Span span : zipkinSpans) {
            if (!validator.isSpanValid(span)) {
                invalidSpanMeter.mark();
                continue;
            }

            final Span haystackSpan = enrich(HaystackDomainConverter.fromZipkinV2(span));
            for (final Dispatcher dispatcher : dispatchers) {
                dispatcher.dispatch(haystackSpan.getTraceId().getBytes(), haystackSpan.toByteArray());
            }
        }
    }

    private Span enrich(final Span span) {
        return Enricher.enrichSpan(span, enrichers);
    }
}
