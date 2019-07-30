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

package com.expedia.www.haystack.agent.pitchfork.service;

import com.expedia.www.haystack.agent.pitchfork.processors.ZipkinSpanProcessorFactory;
import com.expedia.www.haystack.agent.pitchfork.service.config.HttpConfig;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PitchforkService {
    private final static Logger logger = LoggerFactory.getLogger(PitchforkService.class);
    private final Server server;
    private final int port;

    public PitchforkService(final Config config, final ZipkinSpanProcessorFactory processorFactory) {
        final HttpConfig cfg = HttpConfig.from(config);
        this.port = cfg.getPort();
        final QueuedThreadPool threadPool = new QueuedThreadPool(cfg.getMaxThreads(), cfg.getMinThreads(), cfg.getIdleTimeout());
        server = new Server(threadPool);

        final ServerConnector httpConnector = new ServerConnector(server, new HttpConnectionFactory(new HttpConfiguration()));
        httpConnector.setPort(cfg.getPort());
        httpConnector.setIdleTimeout(cfg.getIdleTimeout());
        server.addConnector(httpConnector);

        final ServletContextHandler context = new ServletContextHandler(server, "/");
        addResources(context, processorFactory);

        server.setStopTimeout(cfg.getStopTimeout());
        logger.info("pitchfork has been initialized successfully !");
    }

    private void addResources(final ServletContextHandler context, final ZipkinSpanProcessorFactory processorFactory) {
        ImmutableMap.of(
                "/api/v1/spans", new PitchforkServlet("v1", processorFactory.v1()),
                "/api/v2/spans", new PitchforkServlet("v2", processorFactory.v2()))
                .forEach((endpoint, servlet) -> {
                    logger.info("adding servlet for endpoint={}", endpoint);
                    context.addServlet(new ServletHolder(servlet), endpoint);
                });
    }

    public void start() throws Exception {
        server.start();
        logger.info("pitchfork has been started on port {} ....", port);
    }

    public void stop() throws Exception {
        logger.info("shutting down pitchfork ...");
        server.stop();
    }
}
