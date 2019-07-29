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

import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpanValidator {
    private static final Logger logger = LoggerFactory.getLogger(SpanValidator.class);
    private static final int VALIDATION_DISABLED = -1;

    private static final String ACCEPT_NULL_TIMESTAMPS_CONFIG_KEY = "accept.null.timestamps";
    private static final String MAX_TIMESTAMP_DRIFT_SEC = "max.timestamp.drift.sec";

    private final boolean acceptNullTimestamps;
    private final int maxTimestampDriftSeconds;

    public SpanValidator(final Config config) {
        this.acceptNullTimestamps = config.hasPath(ACCEPT_NULL_TIMESTAMPS_CONFIG_KEY)
                && config.getBoolean(ACCEPT_NULL_TIMESTAMPS_CONFIG_KEY);

        this.maxTimestampDriftSeconds = config.hasPath(MAX_TIMESTAMP_DRIFT_SEC) ?
                config.getInt(MAX_TIMESTAMP_DRIFT_SEC) : VALIDATION_DISABLED;
    }

    public boolean isSpanValid(zipkin2.Span span) {
        if (span.traceId() == null) {
            logger.error("operation=isSpanValid, error='null traceId', service={}, spanId={}",
                    span.localServiceName(),
                    span.id());

            return false;
        }

        if (span.timestamp() == null && !acceptNullTimestamps) {
            logger.error("operation=isSpanValid, error='null timestamp', service={}, traceId={}, spanId={}",
                    span.localServiceName(),
                    span.traceId(),
                    span.id());

            return false;
        }

        if (span.timestamp() != null && maxTimestampDriftSeconds != VALIDATION_DISABLED) {
            long currentTimeInMicros = System.currentTimeMillis() * 1000;

            long driftInMicros = span.timestamp() > currentTimeInMicros
                    ? span.timestamp() - currentTimeInMicros
                    : currentTimeInMicros - span.timestamp();

            long driftInSeconds = driftInMicros / 1000 / 1000;

            if (driftInSeconds > maxTimestampDriftSeconds) {
                logger.error("operation=isSpanValid, error='invalid timestamp', driftInSeconds={} timestamp={}, service={}, traceId={}, spanId={}",
                        driftInSeconds,
                        span.timestamp(),
                        span.localServiceName(),
                        span.traceId(),
                        span.id());

                return false;
            }
        }

        return true;
    }
}
