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

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider;
import com.amazonaws.auth.profile.internal.securitytoken.RoleInfo;
import com.amazonaws.auth.profile.internal.securitytoken.STSProfileCredentialsServiceProvider;
import com.amazonaws.services.kinesis.producer.*;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.expedia.www.haystack.agent.core.Dispatcher;
import com.expedia.www.haystack.agent.core.RateLimitException;
import com.expedia.www.haystack.agent.core.config.ConfigurationHelpers;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.typesafe.config.Config;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;

import static com.expedia.www.haystack.agent.core.config.ConfigurationHelpers.AGENT_NAME_KEY;
import static com.expedia.www.haystack.agent.core.metrics.SharedMetricRegistry.*;

@SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.StdCyclomaticComplexity", "PMD.ModifiedCyclomaticComplexity"})
public class KinesisDispatcher implements Dispatcher {
    private final static Logger LOGGER = LoggerFactory.getLogger(KinesisDispatcher.class);

    static final String STREAM_NAME_KEY = "StreamName";
    static final String OUTSTANDING_RECORD_LIMIT_KEY = "OutstandingRecordsLimit";
    static final String STS_ROLE_ARN = "StsRoleArn";
    static final String AWS_ACCESS_KEY = "AwsAccessKey";
    static final String AWS_SECRET_KEY = "AwsSecretKey";
    static final String AWS_REGION = "Region";

    Timer dispatchTimer;
    Meter dispatchFailureMeter;
    Meter outstandingRecordsError;

    KinesisProducer producer;
    String streamName;
    Integer outstandingRecordsLimit;

    @Override
    public String getName() {
        return "kinesis";
    }

    @Override
    public void dispatch(final byte[] partitionKey, final byte[] record) throws Exception {
        if (producer.getOutstandingRecordsCount() > outstandingRecordsLimit) {
            outstandingRecordsError.mark();
            throw new RateLimitException(String.format("fail to dispatch to kinesis due to rate limit, outstanding records: %d",
                    producer.getOutstandingRecordsCount()));
        } else {
            final Timer.Context timer = dispatchTimer.time();
            final ListenableFuture<UserRecordResult> response = producer.addUserRecord(streamName,
                    new String(partitionKey),
                    ByteBuffer.wrap(record));
            handleAsyncResponse(response, timer);
        }
    }

    @Override
    public void initialize(final Config config) {
        final String agentName = config.hasPath(AGENT_NAME_KEY) ? config.getString(AGENT_NAME_KEY ) : "";

        final Map<String, String> props = ConfigurationHelpers.convertToPropertyMap(config);
        this.streamName = getAndRemoveStreamNameKey(props);
        this.outstandingRecordsLimit = getAndRemoveOutstandingRecordLimitKey(props);

        Validate.notNull(streamName);
        Validate.notNull(outstandingRecordsLimit);
        Validate.notNull(props.get(AWS_REGION));
        props.remove(AGENT_NAME_KEY);

        this.producer = new KinesisProducer(buildKinesisProducerConfiguration(props));
        this.dispatchTimer = newTimer(buildMetricName(agentName, "kinesis.dispatch.timer"));
        this.dispatchFailureMeter = newMeter(buildMetricName(agentName, "kinesis.dispatch.failure"));
        this.outstandingRecordsError = newMeter(buildMetricName(agentName, "kinesis.dispatch.outstanding.records.error"));
        newGauge(buildMetricName(agentName, "kinesis.outstanding.requests"),
                () -> producer.getOutstandingRecordsCount());

        LOGGER.info("Successfully initialized the kinesis dispatcher with config={}", config);
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
    String getAndRemoveStreamNameKey(final Map<String, String> conf) {
        return conf.remove(STREAM_NAME_KEY);
    }

    @VisibleForTesting
    Integer getAndRemoveOutstandingRecordLimitKey(final Map<String, String> conf) {
        final Integer outstandingRecord = Integer.parseInt(conf.get(OUTSTANDING_RECORD_LIMIT_KEY));
        conf.remove(OUTSTANDING_RECORD_LIMIT_KEY);
        return outstandingRecord;
    }

    @VisibleForTesting
    KinesisProducerConfiguration buildKinesisProducerConfiguration(final Map<String, String> conf) {
        final AWSCredentialsProvider credsProvider = buildCredsProvider(conf);
        return fromProperties(ConfigurationHelpers.generatePropertiesFromMap(conf))
                .setCredentialsProvider(credsProvider);
    }

    @VisibleForTesting
    AWSCredentialsProvider buildCredsProvider(final Map<String, String> conf) {
        final Object stsRoleArn = conf.remove(STS_ROLE_ARN);
        final Object awsAccessKey = conf.remove(AWS_ACCESS_KEY);
        final Object awsSecretKey = conf.remove(AWS_SECRET_KEY);

        if (Objects.nonNull(awsAccessKey) && Objects.nonNull(awsSecretKey) && Objects.nonNull(stsRoleArn)) {
            return new STSAssumeRoleSessionCredentialsProvider.Builder(stsRoleArn.toString(), "haystack-agent")
                .withStsClient(
                    AWSSecurityTokenServiceClientBuilder.standard()
                        .withCredentials(
                            new AWSStaticCredentialsProvider(new BasicAWSCredentials(awsAccessKey.toString(), awsSecretKey.toString()))
                        )
                        .withRegion(conf.get(AWS_REGION))
                        .build()
                ).build();
        } else if (Objects.nonNull(awsAccessKey) && Objects.nonNull(awsSecretKey)) {
            LOGGER.info("Using static credential provider using aws access and secret keys");
            return new AWSStaticCredentialsProvider(
                    new BasicAWSCredentials(awsAccessKey.toString(), awsSecretKey.toString()));
        } else {
            if (Objects.nonNull(stsRoleArn)) {
                LOGGER.info("Using aws sts credential provider with role arn={}", stsRoleArn);
                return new STSProfileCredentialsServiceProvider(
                        new RoleInfo().withRoleArn(stsRoleArn.toString()).withRoleSessionName("haystack-agent"));
            } else {
                return DefaultAWSCredentialsProviderChain.getInstance();
            }
        }
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
        }, MoreExecutors.directExecutor());
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

    private static KinesisProducerConfiguration fromProperties(final Properties props) {
        KinesisProducerConfiguration config = new KinesisProducerConfiguration();
        Enumeration<?> propNames = props.propertyNames();
        while (propNames.hasMoreElements()) {
            boolean found = false;
            String key = propNames.nextElement().toString();
            String value = props.getProperty(key);
            for (Method method : KinesisProducerConfiguration.class.getMethods()) {
                if (method.getName().equalsIgnoreCase("set" + key)) {
                    found = true;
                    Class<?> type = method.getParameterTypes()[0];
                    try {
                        if (type == long.class) {
                            method.invoke(config, Long.valueOf(value));
                        } else if (type == int.class) {
                            method.invoke(config, Integer.valueOf(value));
                        } else if (type == boolean.class) {
                            method.invoke(config, Boolean.valueOf(value));
                        } else if (type == String.class) {
                            method.invoke(config, value);
                        }
                    } catch (Exception e) {
                        throw new IllegalArgumentException(
                                String.format("Error trying to set field %s with the value '%s'", key, value), e);
                    }
                }
            }
            if (!found) {
                LOGGER.warn("Property " + key + " ignored as there is no corresponding set method in " +
                        KinesisProducerConfiguration.class.getSimpleName());
            }
        }

        return config;
    }
}
