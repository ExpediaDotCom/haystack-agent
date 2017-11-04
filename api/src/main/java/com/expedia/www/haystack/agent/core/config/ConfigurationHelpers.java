package com.expedia.www.haystack.agent.core.config;

import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/**
 * This dispatcher class is responsible to dispatch the records to Kinesis-Stream.
 */
public class ConfigurationHelpers {


    public static <T> T getPropertyAsType(Map<String, Object> properties, final String propertyName, Class<T> type, final Optional<T> defaultValue) {
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

    public static Properties generatePropertiesFromMap(Map<String, Object> config) {
        Properties properties = new Properties();
        properties.putAll(config);
        return properties;
    }

}
