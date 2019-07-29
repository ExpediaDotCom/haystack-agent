/*
 *  Copyright 2018 Expedia, Inc.
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

package com.expedia.www.haystack.agent.span.enricher;

import com.expedia.open.tracing.Span;

import java.util.List;

public interface Enricher {
    void apply(final Span.Builder span);

    static Span enrichSpan(final Span span, List<Enricher> enrichers) {
        if(enrichers.isEmpty()) {
            return span;
        } else {
            final Span.Builder transformedSpanBuilder = span.toBuilder();
            for (final Enricher enricher : enrichers) {
                enricher.apply(transformedSpanBuilder);
            }
            return transformedSpanBuilder.build();
        }
    }
}
