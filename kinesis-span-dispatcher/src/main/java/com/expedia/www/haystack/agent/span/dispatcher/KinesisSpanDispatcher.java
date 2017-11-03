package com.expedia.www.haystack.agent.span.dispatcher;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.kinesis.producer.KinesisProducer;
import com.amazonaws.services.kinesis.producer.KinesisProducerConfiguration;
import com.expedia.open.tracing.Span;
import com.expedia.www.haystack.agent.core.Dispatcher;
import com.expedia.www.haystack.agent.span.dispatcher.config.KinesisDispatcherConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Map;

public class KinesisSpanDispatcher implements Dispatcher {

    private final String dispatcherName = "kinesis";
    private KinesisProducer producer;
    private KinesisDispatcherConfiguration kinesisConfig;
    private Logger LOGGER = LoggerFactory.getLogger(this.getClass());

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
        producer.flush();
    }

    @Override
    public void initialize(Map<String, Object> conf) {
        kinesisConfig = new KinesisDispatcherConfiguration(conf);
        producer = new KinesisProducer(buildKinesisProducerConfiguration(kinesisConfig));
        LOGGER.info("Successfully initialized the kinesis span dispatcher");

    }

    @Override
    public void close() {
      LOGGER.info("Closing the kinesis span dispatcher now...");
        if(producer!=null){
            producer.destroy();
        }
    }
}
