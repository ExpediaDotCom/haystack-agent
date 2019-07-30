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

package com.expedia.www.haystack.agent.pitchfork.processors;

import com.codahale.metrics.Meter;
import com.expedia.open.tracing.Span;
import com.expedia.www.haystack.agent.core.Dispatcher;
import com.expedia.www.haystack.agent.core.metrics.SharedMetricRegistry;
import com.expedia.www.haystack.agent.span.enricher.Enricher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zipkin2.codec.SpanBytesDecoder;

import java.util.ArrayList;
import java.util.List;

public class ZipkinSpanProcessor {
    private final static Logger logger = LoggerFactory.getLogger(ZipkinSpanProcessor.class);
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

    public void process(byte[] inputBytes) throws Exception {
        final List<zipkin2.Span> zipkinSpans = decode(inputBytes);
        for (final zipkin2.Span span : zipkinSpans) {
            if (!validator.isSpanValid(span)) {
                logger.warn("invalid zipkin span found !");
                invalidSpanMeter.mark();
                continue;
            }

            final Span haystackSpan = enrich(HaystackDomainConverter.fromZipkinV2(span));
            for (final Dispatcher dispatcher : dispatchers) {
                logger.debug("dispatching span to dispatcher {}", dispatcher.getName());
                dispatcher.dispatch(haystackSpan.getTraceId().getBytes(), haystackSpan.toByteArray());
            }
        }
    }

    private List<zipkin2.Span> decode(byte[] inputBytes) {
        final List<zipkin2.Span> decodedSpans = new ArrayList<>();
        try {
            decoder.decodeList(inputBytes, decodedSpans);
        } catch (Exception ex) {
            decoder.decode(inputBytes, decodedSpans);
        }
        return decodedSpans;
    }

    private Span enrich(final Span span) {
        return Enricher.enrichSpan(span, enrichers);
    }
}
