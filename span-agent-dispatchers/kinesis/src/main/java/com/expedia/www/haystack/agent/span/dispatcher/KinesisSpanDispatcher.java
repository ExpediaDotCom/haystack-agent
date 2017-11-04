package com.expedia.www.haystack.agent.span.dispatcher;

import com.amazonaws.services.kinesis.producer.KinesisProducer;
import com.amazonaws.services.kinesis.producer.KinesisProducerConfiguration;
import com.expedia.open.tracing.Span;
import com.expedia.www.haystack.agent.core.Dispatcher;
import com.expedia.www.haystack.agent.span.dispatcher.config.ConfigurationHelpers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;

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
            producer.addUserRecord(streamName,
                    record.getTraceId(),
                    ByteBuffer.wrap(record.toByteArray()));
        }
    }

    @Override
    public void initialize(Map<String, Object> conf) {
        producer = new KinesisProducer(buildKinesisProducerConfiguration(conf));
        streamName =retrieveStreamName(conf);
        outstandingRecordsLimit = retrieveOutstandingRecordsLimitKey(conf);
        LOGGER.info("Successfully initialized the kinesis span dispatcher");
    }


    //Making these functions protected so that they can be tested

    protected String retrieveStreamName(Map<String, Object> conf) {
        return ConfigurationHelpers.getPropertyAsType(conf, STREAM_NAME_KEY, String.class, Optional.empty());
    }

    protected Long retrieveOutstandingRecordsLimitKey(Map<String, Object> conf) {
        return ConfigurationHelpers.getPropertyAsType(conf, OUTSTANDING_RECORD_LIMIT_KEY, Long.class, Optional.empty());
    }

    @Override
    public void close() {
        LOGGER.info("Closing the kinesis span dispatcher now...");
        if (producer != null) {
            producer.flushSync();
            producer.destroy();
        }
    }

    protected KinesisProducerConfiguration buildKinesisProducerConfiguration(Map<String, Object> conf) {
        return KinesisProducerConfiguration.fromProperties(ConfigurationHelpers.generatePropertiesFromMap(conf));
    }


}
