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

package com.expedia.www.haystack.agent.span.spi;

import com.expedia.www.haystack.agent.core.Agent;
import com.expedia.www.haystack.agent.core.Dispatcher;
import com.expedia.www.haystack.agent.core.config.ConfigurationHelpers;
import com.expedia.www.haystack.agent.span.enricher.Enricher;
import com.expedia.www.haystack.agent.span.service.SpanAgentGrpcService;
import com.google.common.annotations.VisibleForTesting;
import com.typesafe.config.Config;
import io.grpc.Server;
import io.grpc.netty.NettyServerBuilder;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class SpanAgent implements Agent {
    private static Logger LOGGER = LoggerFactory.getLogger(SpanAgent.class);

    private List<Dispatcher> dispatchers;
    private Server server;
    private final long KEEP_ALIVE_TIME_IN_SECONDS = 30;

    @Override
    public String getName() {
        return "spans";
    }

    @Override
    public void initialize(final Config config) throws IOException {
        dispatchers = loadAndInitializeDispatchers(config, Thread.currentThread().getContextClassLoader());

        final int port = config.getInt("port");
        final List<Enricher> enrichers = loadSpanEnrichers(config);

        server = NettyServerBuilder
                .forPort(port)
                .directExecutor()
                .permitKeepAliveWithoutCalls(true)
                .permitKeepAliveTime(KEEP_ALIVE_TIME_IN_SECONDS, TimeUnit.SECONDS)
                .addService(new SpanAgentGrpcService(dispatchers, enrichers))
                .build()
                .start();

        LOGGER.info("span agent grpc server started on port {}....", port);

        try {
            server.awaitTermination();
        } catch (InterruptedException ex) {
            LOGGER.error("span agent server has been interrupted with exception", ex);
        }
    }

    @VisibleForTesting
    List<Enricher> loadSpanEnrichers(final Config config) {
        if (config.hasPath("enrichers")) {
            return config.getStringList("enrichers")
                    .stream()
                    .map(clazz -> {
                        try {
                            final Class c = Class.forName(clazz);
                            LOGGER.info("Initializing the span enricher with class name '{}'", clazz);
                            return (Enricher) c.newInstance();
                        } catch (Exception e) {
                            LOGGER.error("Fail to initialize the enricher with clazz name {}", clazz, e);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public void close() {
        try {
            for (final Dispatcher dispatcher : dispatchers) {
                dispatcher.close();
            }
            LOGGER.info("shutting down gRPC server and jmx reporter");
            server.shutdown();
        } catch (Exception ignored) {
        }
    }

    @VisibleForTesting
    List<Dispatcher> loadAndInitializeDispatchers(final Config config, ClassLoader cl) {
        final List<Dispatcher> dispatchers = new ArrayList<>();
        final ServiceLoader<Dispatcher> loadedDispatchers = ServiceLoader.load(Dispatcher.class, cl);

        for (final Dispatcher dispatcher : loadedDispatchers) {
            final Map<String, Config> dispatches = ConfigurationHelpers.readDispatchersConfig(config, getName());
            dispatches
                    .entrySet()
                    .stream()
                    .filter((e) -> e.getKey().equalsIgnoreCase(dispatcher.getName()))
                    .forEach((conf) -> {
                        dispatcher.initialize(conf.getValue());
                        dispatchers.add(dispatcher);
                    });
        }

        Validate.notEmpty(dispatchers, "Span agent dispatchers can't be an empty set");

        return dispatchers;
    }
}
