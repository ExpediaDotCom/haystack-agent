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

import com.expedia.www.haystack.agent.core.BaseAgent;
import com.expedia.www.haystack.agent.span.enricher.Enricher;
import com.expedia.www.haystack.agent.span.service.SpanAgentGrpcService;
import com.typesafe.config.Config;
import io.grpc.Server;
import io.grpc.netty.NettyServerBuilder;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SpanAgent extends BaseAgent {
    private Server server;
    private static final long KEEP_ALIVE_TIME_IN_SECONDS = 30;

    public SpanAgent() {
        super(LoggerFactory.getLogger(SpanAgent.class));
    }

    @Override
    public String getName() {
        return "spans";
    }

    @Override
    public void initialize(final Config config) throws IOException {
        this.dispatchers = loadAndInitializeDispatchers(config, Thread.currentThread().getContextClassLoader(), getName());

        final int port = config.getInt("port");
        final List<Enricher> enrichers = loadSpanEnrichers(config);

        this.server = NettyServerBuilder
                .forPort(port)
                .directExecutor()
                .permitKeepAliveWithoutCalls(true)
                .permitKeepAliveTime(KEEP_ALIVE_TIME_IN_SECONDS, TimeUnit.SECONDS)
                .addService(new SpanAgentGrpcService(dispatchers, enrichers))
                .build()
                .start();

        logger.info("span agent grpc server started on port {}....", port);

        try {
            server.awaitTermination();
        } catch (InterruptedException ex) {
            logger.error("span agent server has been interrupted with exception", ex);
        }
    }

    @Override
    protected void closeInternal() throws Exception {
        this.server.awaitTermination();
    }
}
