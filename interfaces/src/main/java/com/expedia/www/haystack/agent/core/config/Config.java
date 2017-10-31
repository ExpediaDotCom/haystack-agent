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

package com.expedia.www.haystack.agent.core.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import java.util.List;

/**
 * Configuration for agents that are loaded as SPI(service provider interface).
 */
public class Config {
    @JsonProperty("agents")
    private List<AgentConfig> agentConfigs;

    public List<AgentConfig> getAgentConfigs() {
        return agentConfigs;
    }

    public void setAgentConfigs(List<AgentConfig> agentConfigs) {
        this.agentConfigs = agentConfigs;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
