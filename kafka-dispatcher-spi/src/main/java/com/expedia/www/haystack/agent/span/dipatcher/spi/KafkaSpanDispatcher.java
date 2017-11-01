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

package com.expedia.www.haystack.agent.span.dipatcher.spi;

import com.expedia.open.tracing.Span;
import com.expedia.www.haystack.agent.core.Dispatcher;
import org.apache.commons.lang3.Validate;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class KafkaSpanDispatcher implements Dispatcher {
    private static Logger LOGGER = LoggerFactory.getLogger(KafkaSpanDispatcher.class);

    private final static String PRODUCER_TOPIC = "producer.topic";

    private KafkaProducer<byte[], byte[]> producer;
    private String topic;

    @Override
    public String getName() {
        return "kafka";
    }

    @Override
    public void dispatch(final Span record) throws Exception {
        final ProducerRecord<byte[], byte[]> rec = new ProducerRecord<>(
                topic,
                record.getTraceId().getBytes(),
                record.toByteArray());
        producer.send(rec, (metadata, exception) -> {
            if(exception != null) {
                LOGGER.error("Fail to produce the span to kafka with exception", exception);
            }
        });
    }

    @Override
    public void initialize(final Map<String, Object> conf) {
        Validate.notNull(conf.get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG));
        Validate.notNull(conf.get(PRODUCER_TOPIC));

        // remove the producer topic from the configuration and use it during send() call
        topic = conf.remove(PRODUCER_TOPIC).toString();

        producer = new KafkaProducer<>(conf, new ByteArraySerializer(), new ByteArraySerializer());
    }

    @Override
    public void close() {
        LOGGER.info("Closing the kafka span dispatcher now...");
        if(producer != null) {
            producer.close(10, TimeUnit.SECONDS);
            producer = null;
        }
    }
}
