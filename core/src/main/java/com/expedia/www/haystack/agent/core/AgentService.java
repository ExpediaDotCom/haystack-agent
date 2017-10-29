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

import org.apache.commons.lang3.StringUtils;
import com.expedia.www.haystack.agent.config.*;

import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

public class AgentService {
    private AgentService() { }

    private void run(String configProviderName) throws Exception {
        final Config config = loadConfig(configProviderName);
        final ServiceLoader<Agent> agentLoader = ServiceLoader.load(Agent.class);
        for (Agent agent : agentLoader) {
            config.getAgentConfigs()
                    .stream()
                    .filter((c) -> c.getName().equalsIgnoreCase(agent.getName()))
                    .forEach(agent::initialize);
        }
    }

    private Config loadConfig(String configProviderName) throws Exception {
        if(StringUtils.isEmpty(configProviderName)) {
            configProviderName = "file";
        }

        final ServiceLoader<ConfigReader> configLoader = ServiceLoader.load(ConfigReader.class);
        for (ConfigReader reader : configLoader) {
            if (reader.getName().equalsIgnoreCase(configProviderName)) {
                return reader.read();
            }
        }

        throw new ServiceConfigurationError("Fail to load the config provider for type = " + configProviderName);
    }

    public static void main(String[] args) throws Exception {
        new AgentService().run(args[0]);
    }
}