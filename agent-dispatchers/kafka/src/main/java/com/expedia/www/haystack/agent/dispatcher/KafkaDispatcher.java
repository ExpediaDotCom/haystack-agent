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
import com.typesafe.config.Config;
import org.apache.commons.lang3.Validate;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import static com.expedia.www.haystack.agent.core.metrics.SharedMetricRegistry.*;


public class KafkaDispatcher implements Dispatcher {
    private final static Logger LOGGER = LoggerFactory.getLogger(KafkaDispatcher.class);

    private final static String PRODUCER_TOPIC = "producer.topic";

    Timer dispatchTimer;
    Meter dispatchFailure;

    KafkaProducer<byte[], byte[]> producer;
    String topic;

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
    public void initialize(final Config config) {
        final String agentName = config.hasPath("agentName") ? config.getString("agentName") : "";

        Validate.notNull(config.getString(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG));

        // remove the producer topic from the configuration and use it during send() call
        topic = config.getString(PRODUCER_TOPIC);
        producer = new KafkaProducer<>(ConfigurationHelpers.generatePropertiesFromMap(ConfigurationHelpers.convertToPropertyMap(config)),
                new ByteArraySerializer(),
                new ByteArraySerializer());

        dispatchTimer = newTimer(buildMetricName(agentName, "kafka.dispatch.timer"));
        dispatchFailure = newMeter(buildMetricName(agentName, "kafka.dispatch.failure"));

        LOGGER.info("Successfully initialized the kafka dispatcher with config={}", config);
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
}
