package com.expedia.www.haystack.agent.span.dispatcher.config;

import java.util.Map;
import java.util.Optional;

/**
 * This dispatcher class is responsible to dispatch the records to Kinesis-Stream.
 */
public class KinesisDispatcherConfiguration {
    /**
     * Kinesis stream name.
     */
    private final String streamName;
    protected static final String STREAM_NAME_KEY = "streamName";

    /**
     * Region name to which instance of Kinesis trace will be send.
     */
    private final String regionName;
    protected static final String REGION_NAME_KEY = "regionName";


    /**
     * metric level for reporting cloudwatch metrics
     */
    private final String metricLevel;
    protected static final String METRIC_LEVEL_KEY = "metricLevel";
    protected static final String DEFAULT_METRIC_LEVEL = "summary";


    /**
     * The number of outstanding records after which the dispatcher starts dropping spans
     */
    private final long outstandingRecordsLimit;
    protected static final String OUTSTANDING_RECORDS_LIMIT_KEY = "outstandingRecordsLimit";
    protected static final Long DEFAULT_OUTSTANDING_RECORDS_LIMIT = 10000L;


    public KinesisDispatcherConfiguration(Map<String, Object> properties) {
        this.streamName = getPropertyAsType(properties, STREAM_NAME_KEY, String.class, Optional.empty());
        this.regionName = getPropertyAsType(properties, REGION_NAME_KEY, String.class, Optional.empty());
        this.metricLevel = getPropertyAsType(properties, METRIC_LEVEL_KEY, String.class, Optional.of(DEFAULT_METRIC_LEVEL));
        this.outstandingRecordsLimit = getPropertyAsType(properties, OUTSTANDING_RECORDS_LIMIT_KEY, Long.class, Optional.of(DEFAULT_OUTSTANDING_RECORDS_LIMIT));
    }


    private <T> T getPropertyAsType(Map<String, Object> properties, final String propertyName, Class<T> type, final Optional<T> defaultValue) {
        Object value = System.getProperty(propertyName);
        if (value == null) {
            value = properties.get(propertyName);
            if (value == null) {
                if (defaultValue.isPresent()) {
                    return defaultValue.get();
                } else {
                    throw new IllegalArgumentException(String.format("Could not find key for %s in configuration", propertyName));
                }

            }
        }

        if (type.isAssignableFrom(value.getClass())) {
            return type.cast(value);
        }
        if (value instanceof String) {
            if (type.equals(Integer.class)) {
                return type.cast(Integer.valueOf((String) value));
            }

            if (type.equals(Boolean.class)) {
                return type.cast(Boolean.valueOf((String) value));
            }

            if (type.equals(Long.class)) {
                return type.cast(Long.valueOf((String) value));
            }
        }
        throw new IllegalArgumentException(String.format("%s key in configuration can't be cast to type %s", propertyName, type.getSimpleName()));

    }

    public String getMetricLevel() {
        return metricLevel;
    }

    public String getStreamName() {
        return this.streamName;
    }

    public long getOutstandingRecordsLimit() {
        return outstandingRecordsLimit;
    }


    public String getRegionName() {
        return this.regionName;
    }
}
