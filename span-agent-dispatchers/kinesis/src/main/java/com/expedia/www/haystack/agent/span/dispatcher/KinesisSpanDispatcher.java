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
package com.expedia.www.haystack.agent.span.dispatcher;

import com.amazonaws.services.kinesis.producer.*;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.expedia.open.tracing.Span;
import com.expedia.www.haystack.agent.core.Dispatcher;
import com.expedia.www.haystack.agent.core.config.ConfigurationHelpers;
import com.expedia.www.haystack.agent.core.metrics.SharedMetricRegistry;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class KinesisSpanDispatcher implements Dispatcher {
    private final static Logger LOGGER = LoggerFactory.getLogger(KinesisSpanDispatcher.class);

    private static final String STREAM_NAME_KEY = "StreamName";
    private static final String OUTSTANDING_RECORD_LIMIT_KEY = "OutstandingRecordsLimit";

    private final Timer dispatchTimer = SharedMetricRegistry.newTimer("kinesis.dispatch.timer");
    private final Meter dispatchFailureMeter = SharedMetricRegistry.newMeter("kinesis.dispatch.failure");
    private final Meter outstandingRecordsError = SharedMetricRegistry.newMeter("kinesis.dispatch.outstanding.records.error");

    private KinesisProducer producer;
    private String streamName;
    private Long outstandingRecordsLimit;

    @Override
    public String getName() {
        return "kinesis";
    }

    @Override
    public void dispatch(final Span record) throws Exception {
        if (producer.getOutstandingRecordsCount() > outstandingRecordsLimit) {
            outstandingRecordsError.mark();
            throw new RuntimeException(String.format("excessive number of outstanding records: %d",
                    producer.getOutstandingRecordsCount()));
        } else {
            final Timer.Context timer = dispatchTimer.time();
            final ListenableFuture<UserRecordResult> response = producer.addUserRecord(streamName,
                    record.getTraceId(),
                    ByteBuffer.wrap(record.toByteArray()));
            handleAsyncResponse(response, timer);
        }
    }

    @Override
    public void initialize(final Map<String, Object> conf) {
        streamName = getAndRemoveStreamNameKey(conf);
        outstandingRecordsLimit = getAndRemoveOutstandingRecordLimitKey(conf);

        Validate.notNull(streamName);
        Validate.notNull(outstandingRecordsLimit);
        Validate.notNull(conf.get("Region"));

        producer = new KinesisProducer(buildKinesisProducerConfiguration(conf));
        LOGGER.info("Successfully initialized the kinesis span dispatcher");
    }


    @Override
    public void close() {
        LOGGER.info("Closing the kinesis span dispatcher now...");
        if (producer != null) {
            producer.flushSync();
            producer.destroy();
        }
    }


    //Making these functions protected so that they can be tested
    @VisibleForTesting
    String getAndRemoveStreamNameKey(final Map<String, Object> conf) {
        final String streamName = ConfigurationHelpers.getPropertyAsType(conf, STREAM_NAME_KEY, String.class, Optional.empty());
        conf.remove(STREAM_NAME_KEY);
        return streamName;
    }

    @VisibleForTesting
    Long getAndRemoveOutstandingRecordLimitKey(final Map<String, Object> conf) {
        final Long outstandingRecord = ConfigurationHelpers.getPropertyAsType(conf, OUTSTANDING_RECORD_LIMIT_KEY, Long.class, Optional.empty());
        conf.remove(OUTSTANDING_RECORD_LIMIT_KEY);
        return outstandingRecord;
    }

    @VisibleForTesting
    KinesisProducerConfiguration buildKinesisProducerConfiguration(final Map<String, Object> conf) {
        return KinesisProducerConfiguration.fromProperties(ConfigurationHelpers.generatePropertiesFromMap(conf));
    }

    private void handleAsyncResponse(final ListenableFuture<UserRecordResult> response, final Timer.Context timer) {
        Futures.addCallback(response, new FutureCallback<UserRecordResult>() {
            @Override
            public void onSuccess(final UserRecordResult result) {
                timer.close();
                if(!result.isSuccessful()) {
                    dispatchFailureMeter.mark();
                    LOGGER.error("Fail to put the span record to kinesis after attempts={}",
                            formatAttempts(result.getAttempts()));
                }
            }

            @Override
            public void onFailure(final Throwable throwable) {
                timer.close();
                dispatchFailureMeter.mark();

                if (throwable instanceof UserRecordFailedException) {
                    final UserRecordFailedException e = (UserRecordFailedException) throwable;
                    final UserRecordResult result = e.getResult();
                    LOGGER.error("Record failed to put span record to kinesis with attempts={}",
                            formatAttempts(result.getAttempts()), e);
                }
            }
        });
    }

    private String formatAttempts(final List<Attempt> attempts) {
        if (attempts != null) {
            final List<String> attemptStr = attempts
                    .stream()
                    .map(a -> String.format(
                            "Delay after prev attempt: %d ms, "
                                    + "Duration: %d ms, Code: %s, "
                                    + "Message: %s",
                            a.getDelay(), a.getDuration(),
                            a.getErrorCode(),
                            a.getErrorMessage()))
                    .collect(Collectors.toList());
            return StringUtils.join(attemptStr, "\n");
        } else {
            return "none";
        }
    }
}
