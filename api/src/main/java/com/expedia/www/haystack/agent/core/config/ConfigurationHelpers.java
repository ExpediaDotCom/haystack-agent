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

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

public class ConfigurationHelpers {
    // this property is injected into the config object passed to every dispatcher
    // it can be used by the logger or build metric names with agent name as prefix
    public static String AGENT_NAME_KEY = "agentName";

    private final static String HAYSTACK_AGENT_ENV_VAR_PREFIX = "haystack_env_";

    private ConfigurationHelpers() { /* suppress pmd violation */ }

    /**
     * convert the map of [string,string] to properties object
     * @param config map of key value pairs
     * @return a properties object
     */
    public static Properties generatePropertiesFromMap(Map<String, String> config) {
        final Properties properties = new Properties();
        properties.putAll(config);
        return properties;
    }

    /**
     * create typesafe config object by first reading the configuration from environment variables
     * and then doing a fallback on the actual configuration string passed as argument.
     * Environment variables can be used to override the config.
     * @param configStr configuration passed to the app
     * @return final config object with env variables as overrides over actual configuration passed to the app
     */
    public static Config load(final String configStr) {
        return loadFromEnvVars().withFallback(ConfigFactory.parseString(configStr));
    }

    /**
     * parse the agent configurations from the root config
     * @param config main configuration
     * @return map of agentNames and their corresponding config object
     */
    public static Map<String, Config> readAgentConfigs(final Config config) {
        final Map<String, Config> agentConfigs = new HashMap<>();
        final Config agentsConfig = config.getConfig("agents");

        final Set<String> agentNames = new HashSet<>();
        agentsConfig.entrySet().forEach((e) -> agentNames.add(findRootKeyName(e.getKey())));
        agentNames.forEach((name) -> agentConfigs.put(name, agentsConfig.getConfig(name)));
        return agentConfigs;
    }

    /**
     * parse the dispatcher configurations from the agent's config section
     * agent's name is injected into each dispatcher config object, by default
     * @param agentConfig agent's config section
     * @param agentName name of agent
     * @return map of dispatcherNames and their corresponding config object
     */
    public static Map<String, Config> readDispatchersConfig(final Config agentConfig, final String agentName) {
        final Config dispatchers = agentConfig.getConfig("dispatchers");
        final Map<String, Config> dispatchersConfigMap = new HashMap<>();

        final Set<String> dispatcherNames = new HashSet<>();
        dispatchers.entrySet().forEach((e) -> dispatcherNames.add(findRootKeyName(e.getKey())));

        dispatcherNames.forEach((name) -> dispatchersConfigMap.put(name, addAgentNameToConfig(dispatchers.getConfig(name), agentName)));
        return dispatchersConfigMap;
    }

    /**
     * converts typesafe config object to a map of string,string
     * @param conf typesafe config object
     * @return map of key, value pairs
     */
    public static Map<String, String> convertToPropertyMap(final Config conf) {
        final Map<String, String> props = new HashMap<>();
        conf.entrySet().forEach((e) -> props.put(e.getKey(), e.getValue().unwrapped().toString()));
        return props;
    }

    private static Config addAgentNameToConfig(final Config config, final String agentName) {
        return config.withFallback(ConfigFactory.parseString(AGENT_NAME_KEY + " = " + agentName));
    }

    private static boolean isHaystackAgentEnvVar(final String envKey) {
        return envKey.startsWith(HAYSTACK_AGENT_ENV_VAR_PREFIX);
    }

    private static Config loadFromEnvVars() {
        final Map<String, String> envMap = new HashMap<>();
        System.getenv().entrySet().stream()
                .filter((e) -> isHaystackAgentEnvVar(e.getKey()))
                .forEach((e) -> {
                    final String normalizedKey = e.getKey().replaceFirst(HAYSTACK_AGENT_ENV_VAR_PREFIX, "").replace('_', '.');
                    envMap.put(normalizedKey, e.getValue());
                });

        return ConfigFactory.parseMap(envMap);
    }

    // extracts the root keyname, for e.g. if the path given is 'x.y.z' then rootKey is 'x'
    private static String findRootKeyName(final String path) {
        return StringUtils.split(path, ".")[0];
    }
}
