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


import com.expedia.open.tracing.Log;
import com.expedia.open.tracing.Span;
import com.expedia.open.tracing.Tag;
import org.apache.commons.lang3.StringUtils;
import zipkin2.Endpoint;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.Optional.empty;

/**
 * Converter between {@code Zipkin} and {@code Haystack} domains.
 */
@SuppressWarnings("PMD.UnusedPrivateMethod")
class HaystackDomainConverter {

    private static final String SPAN_KIND_TAG_KEY = "span.kind";

    private HaystackDomainConverter() { }
    /**
     * Accepts a span in {@code Zipkin V2} format and returns a span in {@code Haystack} format.
     */
    static Span fromZipkinV2(final zipkin2.Span zipkin) {
        Span.Builder builder = Span.newBuilder()
                .setTraceId(zipkin.traceId())
                .setSpanId(zipkin.id());

        doIfNotNull(zipkin.name(), builder::setOperationName);
        doIfNotNull(zipkin.timestamp(), builder::setStartTime);
        doIfNotNull(zipkin.duration(), builder::setDuration);
        doIfNotNull(zipkin.parentId(), builder::setParentSpanId);
        doIfNotNull(zipkin.localServiceName(), builder::setServiceName);

        builder.addAllTags(addRemoteEndpointAsTags(zipkin.remoteEndpoint()));

        if (!spanKindTagPresent(zipkin)) {
            getTagForKind(zipkin.kind()).ifPresent(builder::addTags);
        }

        if (zipkin.tags() != null && !zipkin.tags().isEmpty()) {
            zipkin.tags().forEach((key, value) -> {
                List<Tag> tagStream = fromZipkinTag(key, value);
                builder.addAllTags(tagStream);
            });
        }

        if (zipkin.annotations() != null && !zipkin.annotations().isEmpty()) {
            zipkin.annotations().forEach(annotation -> {
                final Tag tag = Tag.newBuilder().setKey("annotation").setVStr(annotation.value()).build();
                final Log log = Log.newBuilder().setTimestamp(annotation.timestamp()).addFields(tag).build();
                builder.addLogs(log);
            });
        }
        return builder.build();
    }

    private static List<Tag> addRemoteEndpointAsTags(Endpoint remote) {
        final List<Tag> remoteTags = new ArrayList<>();
        if (remote != null) {
            buildStringTag("remote.service.name", remote::serviceName).ifPresent(remoteTags::add);
            buildStringTag("remote.service.ipv4", remote::ipv4).ifPresent(remoteTags::add);
            buildStringTag("remote.service.ipv6", remote::ipv6).ifPresent(remoteTags::add);
            buildIntTag("remote.service.port", remote::port).ifPresent(remoteTags::add);
        }
        return remoteTags;
    }

    private static Optional<Tag> buildIntTag(final String tagKey, final Supplier<Number> tagValueSupplier) {
        final Number tagValue = tagValueSupplier.get();
        if (tagValue != null) {
            return Optional.of(Tag.newBuilder()
                    .setKey(tagKey)
                    .setVLong((Integer)tagValue)
                    .setType(Tag.TagType.LONG).build());
        }
        return Optional.empty();
    }

    private static Optional<Tag> buildStringTag(final String tagKey, final Supplier<String> tagValueSupplier) {
        final String tagValue = tagValueSupplier.get();
        if (StringUtils.isNotEmpty(tagValue)) {
            return Optional.of(Tag.newBuilder()
                    .setKey(tagKey)
                    .setVStr(tagValue)
                    .setType(Tag.TagType.STRING).build());
        }
        return Optional.empty();
    }

    private static <T> void doIfNotNull(T nullable, Consumer<T> runnable) {
        if (nullable != null) {
            runnable.accept(nullable);
        }
    }

    private static boolean spanKindTagPresent(zipkin2.Span zipkinSpan) {
        return zipkinSpan.tags() != null &&
            !zipkinSpan.tags().isEmpty() &&
            zipkinSpan.tags().keySet().contains(SPAN_KIND_TAG_KEY);
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
                    .setKey(SPAN_KIND_TAG_KEY)
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
