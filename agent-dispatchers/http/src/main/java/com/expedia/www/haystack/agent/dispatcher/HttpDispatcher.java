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

import static com.expedia.www.haystack.agent.core.metrics.SharedMetricRegistry.buildMetricName;
import static com.expedia.www.haystack.agent.core.metrics.SharedMetricRegistry.newMeter;
import static com.expedia.www.haystack.agent.core.metrics.SharedMetricRegistry.newTimer;

import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.expedia.www.haystack.agent.core.Dispatcher;
import com.typesafe.config.Config;
import okhttp3.ConnectionPool;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HttpDispatcher implements Dispatcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpDispatcher.class);

    private static final MediaType PROTOBUF = MediaType.get("application/octet-stream");
    private static final String URL = "url";
    private static final String CALL_TIMEOUT_IN_MILLIS = "client.timeoutmillis";
    private static final String MAX_IDLE_CONNECTIONS = "client.connectionpool.idleconnections.max";
    private static final String KEEP_ALIVE_DURATION_IN_MINUTES = "client.connectionpool.keepaliveminutes";

    Timer dispatchTimer;
    Meter dispatchFailure;

    OkHttpClient client;
    String url;

    @Override
    public String getName() {
        return "http";
    }

    @Override
    public void dispatch(final byte[] ignored, final byte[] data) throws Exception {
        RequestBody body = RequestBody.create(PROTOBUF, data);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        try (Timer.Context timer = dispatchTimer.time(); Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                dispatchFailure.mark();
                LOGGER.error("Fail to post the record to the http collector with status code {}", response.code());
            }
        } catch (Exception e) {
            dispatchFailure.mark();
            LOGGER.error("Fail to post the record to the http collector", e);
        }
    }

    @Override
    public void initialize(final Config config) {
        final String agentName = config.hasPath("agentName") ? config.getString("agentName") : "";

        Validate.notNull(config.getString(URL));
        url = config.getString(URL);

        client = buildClient(config);

        dispatchTimer = newTimer(buildMetricName(agentName, "http.dispatch.timer"));
        dispatchFailure = newMeter(buildMetricName(agentName, "http.dispatch.failure"));

        LOGGER.info("Successfully initialized the http dispatcher with config={}", config);
    }

    private OkHttpClient buildClient(Config config) {
        final int callTimeoutInMilliseconds = config.hasPath(CALL_TIMEOUT_IN_MILLIS) ? config.getInt(CALL_TIMEOUT_IN_MILLIS) : 1000;
        final int maxIdleConnections = config.hasPath(MAX_IDLE_CONNECTIONS) ? config.getInt(MAX_IDLE_CONNECTIONS) : 5;
        final int keepAliveDuration = config.hasPath(KEEP_ALIVE_DURATION_IN_MINUTES) ? config.getInt(KEEP_ALIVE_DURATION_IN_MINUTES) : 5;

        return new OkHttpClient.Builder()
                .callTimeout(callTimeoutInMilliseconds, TimeUnit.MILLISECONDS)
                .connectionPool(new ConnectionPool(maxIdleConnections, keepAliveDuration, TimeUnit.SECONDS))
                .build();
    }

    @Override
    public void close() {
        LOGGER.info("Closing the http dispatcher now...");
    }
}
