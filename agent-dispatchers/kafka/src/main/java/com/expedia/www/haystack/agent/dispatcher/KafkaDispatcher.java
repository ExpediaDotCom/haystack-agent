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

import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.expedia.www.haystack.agent.core.Dispatcher;
import com.expedia.www.haystack.agent.core.config.ConfigurationHelpers;
import com.expedia.www.haystack.agent.core.metrics.SharedMetricRegistry;
import com.google.common.annotations.VisibleForTesting;
import com.typesafe.config.Config;
import org.apache.commons.lang3.Validate;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class KafkaDispatcher implements Dispatcher {
    private final static Logger LOGGER = LoggerFactory.getLogger(KafkaDispatcher.class);

    private final static String PRODUCER_TOPIC = "producerTopic";
    private final Timer dispatchTimer = SharedMetricRegistry.newTimer("kafka.dispatch.timer");
    private final Meter dispatchFailure = SharedMetricRegistry.newMeter("kafka.dispatch.failure");

    private KafkaProducer<byte[], byte[]> producer;
    private String topic;

    @Override
    public String getName() {
        return "kafka";
    }

    @Override
    public void dispatch(final byte[] partitionKey, final byte[] data) throws Exception {
        final Timer.Context timer = dispatchTimer.time();
        final ProducerRecord<byte[], byte[]> rec = new ProducerRecord<>(
                topic,
                partitionKey,
                data);
        producer.send(rec, (metadata, exception) -> {
            timer.close();
            if(exception != null) {
                dispatchFailure.mark();
                LOGGER.error("Fail to produce the record to kafka with exception", exception);
            }
        });
    }

    @Override
    public void initialize(final Config conf) {
        Validate.notNull(conf.getString(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG));

        // remove the producer topic from the configuration and use it during send() call
        setTopic(conf.getString(PRODUCER_TOPIC));
        setKafkaProducer(new KafkaProducer<>(ConfigurationHelpers.generatePropertiesFromMap(ConfigurationHelpers.convertToPropertyMap(conf)),
                new ByteArraySerializer(),
                new ByteArraySerializer()));
    }

    @Override
    public void close() {
        LOGGER.info("Closing the kafka dispatcher now...");
        if(producer != null) {
            producer.flush();
            producer.close(10, TimeUnit.SECONDS);
            producer = null;
        }
    }

    @VisibleForTesting
    void setKafkaProducer(final KafkaProducer<byte[], byte[]> producer) {
        this.producer = producer;
    }

    @VisibleForTesting
    void setTopic(final String topic) {
        this.topic = topic;
    }
}
