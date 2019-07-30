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


import com.expedia.open.tracing.Span;
import com.expedia.open.tracing.Tag;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static java.util.Optional.empty;

/**
 * Converter between {@code Zipkin} and {@code Haystack} domains.
 */
@SuppressWarnings("PMD.UnusedPrivateMethod")
public class HaystackDomainConverter {

    private HaystackDomainConverter() { }
    /**
     * Accepts a span in {@code Zipkin V2} format and returns a span in {@code Haystack} format.
     */
    public static Span fromZipkinV2(final zipkin2.Span zipkin) {
        Span.Builder builder = Span.newBuilder()
                .setTraceId(zipkin.traceId())
                .setSpanId(zipkin.id());

        doIfNotNull(zipkin.name(), builder::setOperationName);
        doIfNotNull(zipkin.timestamp(), builder::setStartTime);
        doIfNotNull(zipkin.duration(), builder::setDuration);
        doIfNotNull(zipkin.parentId(), builder::setParentSpanId);
        doIfNotNull(zipkin.localServiceName(), builder::setServiceName);

        if (zipkin.tags() != null && !zipkin.tags().isEmpty()) {
            zipkin.tags().forEach((key, value) -> {
                List<Tag> tagStream = fromZipkinTag(key, value);
                builder.addAllTags(tagStream);
            });
        }

        getTagForKind(zipkin.kind()).ifPresent(builder::addTags);

        return builder.build();
    }

    private static <T> void doIfNotNull(T nullable, Consumer<T> runnable) {
        if (nullable != null) {
            runnable.accept(nullable);
        }
    }

    private static Optional<Tag> getTagForKind(zipkin2.Span.Kind kind) {
        String value;

        if (kind != null) {
            switch (kind) {
                case CLIENT:
                    value = "client";
                    break;
                case SERVER:
                    value = "server";
                    break;
                case CONSUMER:
                    value = "consumer";
                    break;
                case PRODUCER:
                    value = "producer";
                    break;
                default:
                    throw new RuntimeException(String.format("Fail to recognize the span kind %s!", kind));
            }

            return Optional.of(Tag.newBuilder()
                    .setKey("span.kind")
                    .setVStr(value)
                    .setType(Tag.TagType.STRING)
                    .build());
        } else {
            return empty();
        }
    }

    private static List<Tag> fromZipkinTag(String key, String value) {
        if ("error".equalsIgnoreCase(key)) {
            // Zipkin error tags are Strings where as in Haystack they're Booleans
            // Since a Zipkin error tag may contain relevant information about the error we expand it into two tags (error + error message)
            Tag errorTag = Tag.newBuilder()
                    .setKey(key)
                    .setVBool(true)
                    .setType(Tag.TagType.BOOL)
                    .build();

            Tag errorMessageTag = Tag.newBuilder()
                    .setKey("error_msg")
                    .setVStr(value)
                    .setType(Tag.TagType.STRING)
                    .build();

            return Arrays.asList(errorTag, errorMessageTag);
        }

        final Tag tag = Tag.newBuilder()
                .setKey(key)
                .setVStr(value)
                .setType(Tag.TagType.STRING)
                .build();

        return Collections.singletonList(tag);
    }
}