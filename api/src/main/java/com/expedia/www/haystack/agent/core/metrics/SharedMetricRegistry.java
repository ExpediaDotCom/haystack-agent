package com.expedia.www.haystack.agent.core.metrics;

import com.codahale.metrics.*;

public class SharedMetricRegistry {
    private final static String MetricRegistryName = "HAYSTACK_AGENT_METRIC_REGISTRY";

    private static JmxReporter reporter = null;

    /**
     * start the jmx reporter for shared metric registry
     */
    public synchronized static void startJmxMetricReporter() {
        if(reporter == null) {
            reporter = JmxReporter.forRegistry(get()).build();
            reporter.start();
        }
    }

    /**
     * close the jmx reporter for shared metric registry.
     * The agent should close the jmx reporter
     */
    public synchronized static void closeJmxMetricReporter() {
        if (reporter != null) {
            reporter.close();
            reporter = null;
        }
    }

    /**
     * @param name timer name
     * @return a new timer object
     */
    public static Timer newTimer(final String name) {
        return get().timer(name);
    }

    /**
     * @param name meter name
     * @return a new meter object
     */
    public static Meter newMeter(final String name) {
        return get().meter(name);
    }

    private static MetricRegistry get() {
        return SharedMetricRegistries.getOrCreate(MetricRegistryName);
    }
}
