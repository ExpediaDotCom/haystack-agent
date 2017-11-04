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
package com.expedia.www.haystack.agent.core;

import com.expedia.www.haystack.agent.core.config.AgentConfig;
import com.expedia.www.haystack.agent.core.config.Config;
import com.expedia.www.haystack.agent.core.config.ConfigReader;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class AgentLoader {

    private static Logger LOGGER = LoggerFactory.getLogger(AgentLoader.class);

    AgentLoader() { }

    void run(String configProviderName, final Map<String, String> configProviderArgs) throws Exception {
        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        final Config config = loadConfig(configProviderName, configProviderArgs, cl);
        loadAgents(config, cl);
    }

    @VisibleForTesting
    void loadAgents(final Config config, ClassLoader cl) throws Exception {
        final ServiceLoader<Agent> agentLoader = ServiceLoader.load(Agent.class, cl);

        final Set<String> agentConfigsLoaded = Sets.newHashSet();

        for (final Agent agent : agentLoader) {
            final Optional<AgentConfig> agentConfig = config.getAgentConfigs()
                    .stream()
                    .filter((c) -> c.getName().equalsIgnoreCase(agent.getName())).findFirst();
            if(agentConfig.isPresent()) {
                LOGGER.info("Initializing agent with name={} and config={}", agent.getName(), agentConfig.get());
                agent.initialize(agentConfig.get());
                agentConfigsLoaded.add(agentConfig.get().getName());
            }
        }

        final String notLoadableAgentNames =  config.getAgentConfigs()
                .stream()
                .map(AgentConfig::getName)
                .filter((c) -> !agentConfigsLoaded.contains(c))
                .reduce("", (prev, next) -> next + "," + prev);

        if(notLoadableAgentNames.length() > 0) {
            throw new ServiceConfigurationError("Fail to load the agents with names=" + notLoadableAgentNames);
        }
    }

    @VisibleForTesting
    Config loadConfig(final String configProviderName,
                      final Map<String, String> configProviderArgs,
                      final ClassLoader cl) throws Exception {
        final ServiceLoader<ConfigReader> configLoader = ServiceLoader.load(ConfigReader.class, cl);
        for (final ConfigReader reader : configLoader) {
            if (reader.getName().equalsIgnoreCase(configProviderName)) {
                return reader.read(configProviderArgs);
            }
        }
        throw new ServiceConfigurationError("Fail to load the config provider for type = " + configProviderName);
    }

    @VisibleForTesting
    static ImmutablePair<String, Map<String, String>> parseArgs(String[] args) {
        final Map<String, String> configProviderArgs = new HashMap<>();
        String configProviderName = "file";

        if(args != null) {
            for(int idx = 0; idx < args.length; idx = idx + 2) {
                if(Objects.equals(args[idx], "--config-provider")) {
                    configProviderName = args[idx + 1];
                } else {
                    configProviderArgs.put(args[idx], args[idx + 1]);
                }
            }
        }

        return ImmutablePair.of(configProviderName, configProviderArgs);
    }

    public static void main(String[] args) throws Exception {
        final ImmutablePair<String, Map<String, String>> configReader = parseArgs(args);
        new AgentLoader().run(configReader.getKey(), configReader.getValue());
    }
}