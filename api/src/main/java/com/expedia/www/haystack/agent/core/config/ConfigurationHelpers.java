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
    private final static String HAYSTACK_AGENT_ENV_VAR_PREFIX = "haystack_env_";

    private ConfigurationHelpers() { /* suppress pmd violation */ }

    public static Properties generatePropertiesFromMap(Map<String, String> config) {
        final Properties properties = new Properties();
        properties.putAll(config);
        return properties;
    }

    public static Config load(final String configStr) {
        return loadFromEnvVars().withFallback(ConfigFactory.parseString(configStr));
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

    public static Map<String, Config> readAgentConfigs(final Config config) {
        final Map<String, Config> agentConfigs = new HashMap<>();
        final Config agentsConfig = config.getConfig("agents");

        final Set<String> agentNames = new HashSet<>();
        agentsConfig.entrySet().forEach((e) -> agentNames.add(findRootKeyName(e.getKey())));
        agentNames.forEach((name) -> agentConfigs.put(name, agentsConfig.getConfig(name)));
        return agentConfigs;
    }

    public static Map<String, Config> readDispatchersConfig(final Config config) {
        final Config dispatchers = config.getConfig("dispatchers");
        final Map<String, Config> dispatchersConfigMap = new HashMap<>();

        final Set<String> dispatcherNames = new HashSet<>();
        dispatchers.entrySet().forEach((e) -> dispatcherNames.add(findRootKeyName(e.getKey())));
        dispatcherNames.forEach((name) -> dispatchersConfigMap.put(name, dispatchers.getConfig(name)));
        return dispatchersConfigMap;
    }

    public static Map<String, String> convertToPropertyMap(final Config conf) {
        final Map<String, String> props = new HashMap<>();
        conf.entrySet().forEach((e) -> props.put(e.getKey(), e.getValue().unwrapped().toString()));
        return props;
    }

    private static String findRootKeyName(final String path) {
        return StringUtils.split(path, ".")[0];
    }
}
