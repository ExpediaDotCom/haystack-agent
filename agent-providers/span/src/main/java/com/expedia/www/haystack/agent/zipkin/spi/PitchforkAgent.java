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

package com.expedia.www.haystack.agent.zipkin.spi;

import com.expedia.www.haystack.agent.core.BaseAgent;
import com.expedia.www.haystack.agent.span.enricher.Enricher;
import com.expedia.www.haystack.agent.zipkin.processors.SpanValidator;
import com.expedia.www.haystack.agent.zipkin.processors.ZipkinSpanProcessorFactory;
import com.expedia.www.haystack.agent.zipkin.service.PitchforkService;
import com.typesafe.config.Config;
import org.slf4j.LoggerFactory;

import java.util.List;

public class PitchforkAgent extends BaseAgent {

    private PitchforkService httpService;

    public PitchforkAgent() {
        super(LoggerFactory.getLogger(PitchforkAgent.class));
    }

    @Override
    public String getName() {
        return "pitchfork";
    }

    @Override
    public void initialize(Config config) throws Exception {
        dispatchers = loadAndInitializeDispatchers(config, Thread.currentThread().getContextClassLoader(), getName());
        final List<Enricher> enrichers = loadSpanEnrichers(config);
        final SpanValidator validator = buildSpanValidator(config);

        final ZipkinSpanProcessorFactory factory = new ZipkinSpanProcessorFactory(validator, dispatchers, enrichers);
        httpService = new PitchforkService(config, factory);
        httpService.start();
    }

    private SpanValidator buildSpanValidator(final Config config) {
        return new SpanValidator(config);
    }

    @Override
    protected void closeInternal() throws Exception {
        httpService.stop();
    }
}
