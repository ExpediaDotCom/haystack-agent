/*
 *  Copyright 2019 Expedia, Inc.
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

package com.expedia.www.haystack.agent.pitchfork.service.config;

import com.typesafe.config.Config;
import org.apache.commons.lang3.Validate;

public class HttpConfig {
    private final static String PORT_CONFIG_KEY = "port";
    private final static String MAX_THREADS_CONFIG_KEY = "http.threads.max";
    private final static String MIN_THREADS_CONFIG_KEY = "http.threads.min";
    private final static String IDLE_TIMEOUT_MILLIS_CONFIG_KEY = "idle.timeout.ms";
    private final static String STOP_TIMEOUT_MILLIS_CONFIG_KEY = "stop.timeout.ms";

    private int port;
    private int maxThreads;
    private int minThreads;
    private int idleTimeout;
    private int stopTimeout;

    HttpConfig(int port, int minThreads, int maxThreads, int idleTimeout, int stopTimeout) {
        Validate.isTrue(minThreads <= maxThreads, "min threads has to be less than or equal to max threads count");
        Validate.isTrue(port > 0, "http port should be > 0");
        Validate.isTrue(idleTimeout > 0, "idle timeout should be > 0");
        Validate.isTrue(stopTimeout > 0, "stop timeout should be > 0");

        this.port = port;
        this.maxThreads = maxThreads;
        this.minThreads = minThreads;
        this.idleTimeout = idleTimeout;
        this.stopTimeout = stopTimeout;
    }

    public int getPort() {
        return port;
    }

    public int getMaxThreads() {
        return maxThreads;
    }

    public int getMinThreads() {
        return minThreads;
    }

    public int getIdleTimeout() {
        return idleTimeout;
    }

    public int getStopTimeout() {
        return stopTimeout;
    }

    @SuppressWarnings("PMD.NPathComplexity")
    public static HttpConfig from(Config config) {
        int port = config.hasPath(PORT_CONFIG_KEY) ? config.getInt(PORT_CONFIG_KEY) : 9411;
        int maxThreads = config.hasPath(MAX_THREADS_CONFIG_KEY) ? config.getInt(MAX_THREADS_CONFIG_KEY) : 16;
        int minThreads = config.hasPath(MIN_THREADS_CONFIG_KEY) ? config.getInt(MIN_THREADS_CONFIG_KEY) : 2;
        int idleTimeout = config.hasPath(IDLE_TIMEOUT_MILLIS_CONFIG_KEY) ? config.getInt(IDLE_TIMEOUT_MILLIS_CONFIG_KEY) : 60000;
        int stopTimeout = config.hasPath(STOP_TIMEOUT_MILLIS_CONFIG_KEY) ? config.getInt(STOP_TIMEOUT_MILLIS_CONFIG_KEY) : 30000;
        return new HttpConfig(port, minThreads, maxThreads, idleTimeout, stopTimeout);
    }
}
