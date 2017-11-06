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
import com.expedia.www.haystack.agent.core.span.Dispatcher;
import com.expedia.www.haystack.agent.core.config.AgentConfig;
import com.expedia.www.haystack.agent.core.config.ConfigurationHelpers;
import com.expedia.www.haystack.agent.core.metrics.SharedMetricRegistry;
import com.expedia.www.haystack.agent.span.service.SpanAgentGrpcService;
import com.google.common.annotations.VisibleForTesting;
import io.grpc.internal.ServerImpl;
import io.grpc.netty.NettyServerBuilder;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

public class SpanAgent implements Agent {
    private static Logger LOGGER = LoggerFactory.getLogger(SpanAgent.class);

    @Override
    public String getName() {
        return "spans";
    }

    @Override
    public void initialize(final AgentConfig config) throws IOException {
        final List<Dispatcher> dispatchers = getAndInitializeDispatchers(config, Thread.currentThread().getContextClassLoader());

        final Integer port = ConfigurationHelpers.getPropertyAsType(config.getProps(),
                "port",
                Integer.class,
                Optional.empty());

        final ServerImpl server = NettyServerBuilder
                .forPort(port)
                .directExecutor()
                .addService(new SpanAgentGrpcService(dispatchers))
                .build()
                .start();

        LOGGER.info("span agent grpc server started ....");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            for (final Dispatcher dispatcher : dispatchers) {
                dispatcher.close();
            }
            LOGGER.info("shutting down gRPC server and jmx reporter");
            server.shutdown();
            SharedMetricRegistry.closeJmxMetricReporter();
        }));

        try {
            server.awaitTermination();
        } catch (InterruptedException ex) {
            LOGGER.error("span agent server has been interrupted with exception", ex);
        }
    }

    @VisibleForTesting
    List<Dispatcher> getAndInitializeDispatchers(final AgentConfig config, ClassLoader cl) {
        final List<Dispatcher> dispatchers = new ArrayList<>();
        final ServiceLoader<Dispatcher> loadedDispatchers = ServiceLoader.load(Dispatcher.class, cl);

        for (final Dispatcher dispatcher : loadedDispatchers) {
            config.getDispatchers()
                    .entrySet()
                    .stream()
                    .filter((e) -> e.getKey().equalsIgnoreCase(dispatcher.getName()))
                    .forEach((conf) -> {
                        dispatcher.initialize(conf.getValue());
                        dispatchers.add(dispatcher);
                    });
        }

        Validate.notEmpty(dispatchers, "Dispatchers can't be an empty set");

        return dispatchers;
    }
}
