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
import com.expedia.www.haystack.agent.core.config.AgentConfig;
import com.expedia.www.haystack.agent.core.dispatcher.Dispatcher;
import com.expedia.www.haystack.agent.span.service.SpanAgentGrpcService;
import io.grpc.internal.ServerImpl;
import io.grpc.netty.NettyServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

public class SpanAgent implements Agent {
    private static Logger LOGGER = LoggerFactory.getLogger(SpanAgent.class);

    @Override
    public String getName() {
        return "spans";
    }

    @Override
    public void initialize(final AgentConfig config) throws IOException {
        final List<Dispatcher> dispatchers = new ArrayList<>();

        final ServiceLoader<Dispatcher> loadedDispatchers = ServiceLoader.load(Dispatcher.class);
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

        final ServerImpl server = NettyServerBuilder
                .forPort(Integer.parseInt(config.getProps().get("port").toString()))
                .addService(new SpanAgentGrpcService(dispatchers))
                .build()
                .start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("shutting down gRPC server since JVM is shutting down");
            server.shutdown();
            LOGGER.info("span agent server shut down complete");
        }));

        try {
            server.awaitTermination();
        } catch (InterruptedException ex) {
            LOGGER.error("span agent server has been interrupted with exception", ex);
        }
    }
}
