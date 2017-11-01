package com.expedia.www.haystack.agent.span.dispatcher;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.kinesis.producer.KinesisProducer;
import com.amazonaws.services.kinesis.producer.KinesisProducerConfiguration;
import com.expedia.open.tracing.Span;
import com.expedia.www.haystack.agent.core.dispatcher.Dispatcher;
import com.expedia.www.haystack.agent.span.dispatcher.config.KinesisDispatcherConfiguration;

import java.nio.ByteBuffer;
import java.util.Map;

public class KinesisSpanDispatcher implements Dispatcher {

    private final String dispatcherName = "kinesis";
    private KinesisProducer producer;
    private KinesisDispatcherConfiguration kinesisConfig;

    private KinesisProducerConfiguration buildKinesisProducerConfiguration(
            KinesisDispatcherConfiguration dispatcherConfig) {
        final KinesisProducerConfiguration kinesisProdConfig = new KinesisProducerConfiguration();
        kinesisProdConfig.setRegion(dispatcherConfig.getRegionName());
        kinesisProdConfig.setMetricsLevel(dispatcherConfig.getMetricLevel());
        kinesisProdConfig.setCredentialsProvider(DefaultAWSCredentialsProviderChain.getInstance());
        return kinesisProdConfig;
    }

    @Override
    public String getName() {
        return dispatcherName;
    }

    @Override
    public void dispatch(Span record) throws Exception {
        if (producer.getOutstandingRecordsCount() > kinesisConfig.getOutstandingRecordsLimit()) {
            throw new RuntimeException(String.format("excessive number of outstanding records: %d", producer.getOutstandingRecordsCount()));
        } else {
            producer.addUserRecord(kinesisConfig.getStreamName(),
                    record.getTraceId(),
                    ByteBuffer.wrap(record.toByteArray()));
        }
    }

    @Override
    public void initialize(Map<String, Object> conf) {
        kinesisConfig = new KinesisDispatcherConfiguration(conf);
        producer = new KinesisProducer(buildKinesisProducerConfiguration(kinesisConfig));
    }
}
