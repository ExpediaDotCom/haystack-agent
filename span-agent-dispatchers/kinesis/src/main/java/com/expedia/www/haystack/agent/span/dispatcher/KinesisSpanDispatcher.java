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

import com.amazonaws.services.kinesis.producer.KinesisProducer;
import com.amazonaws.services.kinesis.producer.KinesisProducerConfiguration;
import com.amazonaws.services.kinesis.producer.UserRecordFailedException;
import com.amazonaws.services.kinesis.producer.UserRecordResult;
import com.expedia.open.tracing.Span;
import com.expedia.www.haystack.agent.core.Dispatcher;
import com.expedia.www.haystack.agent.core.config.ConfigurationHelpers;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class KinesisSpanDispatcher implements Dispatcher {

    public static final String STREAM_NAME_KEY = "StreamName";
    public static final String OUTSTANDING_RECORD_LIMIT_KEY = "OutstandingRecordsLimit";

    private final String dispatcherName = "kinesis";
    private KinesisProducer producer;

    private String streamName;
    protected Long outstandingRecordsLimit;

    private Logger LOGGER = LoggerFactory.getLogger(this.getClass());


    @Override
    public String getName() {
        return dispatcherName;
    }

    @Override
    public void dispatch(Span record) throws Exception {
        if (producer.getOutstandingRecordsCount() > outstandingRecordsLimit) {
            throw new RuntimeException(String.format("excessive number of outstanding records: %d", producer.getOutstandingRecordsCount()));
        } else {
            ListenableFuture<UserRecordResult> response = producer.addUserRecord(streamName,
                    record.getTraceId(),
                    ByteBuffer.wrap(record.toByteArray()));
            Futures.addCallback(response, new FutureCallback<UserRecordResult>() {
                @Override
                public void onSuccess(UserRecordResult result) {
                    //TODO : Add a performance counter for put success
                }

                @Override
                public void onFailure(Throwable throwable) {
                    if (throwable instanceof UserRecordFailedException) {
                        UserRecordFailedException e =
                                (UserRecordFailedException) throwable;
                        UserRecordResult result = e.getResult();

                        String errorList =
                                StringUtils.join(result.getAttempts().stream()
                                        .map(a -> String.format(
                                                "Delay after prev attempt: %d ms, "
                                                        + "Duration: %d ms, Code: %s, "
                                                        + "Message: %s",
                                                a.getDelay(), a.getDuration(),
                                                a.getErrorCode(),
                                                a.getErrorMessage()))
                                        .collect(Collectors.toList()), "n");

                        LOGGER.error(String.format(
                                "Record failed to put payload=%s, attempts:n%s",
                                Arrays.toString(record.toByteArray()), errorList));
                    }
                }

                ;
            });
        }

    }

    @Override
    public void initialize(Map<String, Object> conf) {
        producer = new KinesisProducer(buildKinesisProducerConfiguration(conf));
        streamName = retrieveStreamName(conf);
        outstandingRecordsLimit = retrieveOutstandingRecordsLimitKey(conf);
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
    String retrieveStreamName(Map<String, Object> conf) {
        String streamName = ConfigurationHelpers.getPropertyAsType(conf, STREAM_NAME_KEY, String.class, Optional.empty());
        conf.remove(STREAM_NAME_KEY);
        return streamName;
    }
    @VisibleForTesting
    Long retrieveOutstandingRecordsLimitKey(Map<String, Object> conf) {
        Long outstandingRecord = ConfigurationHelpers.getPropertyAsType(conf, OUTSTANDING_RECORD_LIMIT_KEY, Long.class, Optional.empty());
        conf.remove(OUTSTANDING_RECORD_LIMIT_KEY);
        return outstandingRecord;
    }

    @VisibleForTesting
    KinesisProducerConfiguration buildKinesisProducerConfiguration(Map<String, Object> conf) {
        return KinesisProducerConfiguration.fromProperties(ConfigurationHelpers.generatePropertiesFromMap(conf));
    }


}
