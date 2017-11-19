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

package com.expedia.www.haystack.agent.core.metrics;

import com.codahale.metrics.*;
import org.apache.commons.lang3.StringUtils;

public class SharedMetricRegistry {
    private final static String MetricRegistryName = "HAYSTACK_AGENT_METRIC_REGISTRY";
    private final static Object lock = new Object();

    private static JmxReporter reporter;

    private SharedMetricRegistry() { /* suppress pmd violation */ }

    /**
     * start the jmx reporter for shared metric registry
     */
    public static void startJmxMetricReporter() {
        synchronized (lock) {
            if (reporter == null) {
                reporter = JmxReporter.forRegistry(get()).build();
                reporter.start();
            }
        }
    }

    /**
     * close the jmx reporter for shared metric registry.
     * The agent should close the jmx reporter
     */
    public static void closeJmxMetricReporter() {
        synchronized (lock) {
            if (reporter != null) {
                reporter.close();
                reporter = null;
            }
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

    public static <T> Gauge newGauge(final String name, final Gauge<T> gauge) {
        return get().gauge(name, () -> gauge);
    }

    /**
     * adds agentName(if non-empty) as prefix to metricName
     * @param agentName name of agent
     * @param metricName name of metric
     * @return complete metric name
     */
    public static String buildMetricName(final String agentName, final String metricName) {
        return StringUtils.isEmpty(agentName) ? metricName : agentName + "." + metricName;
    }

    private static MetricRegistry get() {
        return SharedMetricRegistries.getOrCreate(MetricRegistryName);
    }
}
