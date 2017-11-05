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

package com.expedia.www.haystack.agent.config.spi;

import com.expedia.www.haystack.agent.core.config.Config;
import com.expedia.www.haystack.agent.core.config.ConfigReader;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;

/**
 * loads the config for all agents from file. It uses system property, environment variable and default config file
 * in the given order to find out config file path.
 */
public class FileConfigReader implements ConfigReader {
    private final static Logger LOGGER = LoggerFactory.getLogger(FileConfigReader.class);

    private final static String HAYSTACK_AGENT_CONFIG_FILE_PATH = "HAYSTACK_AGENT_CONFIG_FILE_PATH";

    @Override
    public String getName() {
        return "file";
    }

    @Override
    public Config read(final Map<String, String> args) throws Exception {
        String configFilePath = args.get("--file-path");
        if(StringUtils.isEmpty(configFilePath)) {
            configFilePath = System.getProperty(HAYSTACK_AGENT_CONFIG_FILE_PATH);
            if (StringUtils.isEmpty(configFilePath)) {
                configFilePath = System.getenv(HAYSTACK_AGENT_CONFIG_FILE_PATH);
                if (StringUtils.isEmpty(configFilePath)) {
                    LOGGER.info("Neither system property nor environment variable is found for haystack agent config file, falling to default file path={}", configFilePath);
                    throw new RuntimeException("Fail to find a valid config file path");
                } else {
                    LOGGER.info("Environment variable for haystack agent config file path found with value={}", configFilePath);
                }
            } else {
                LOGGER.info("System property for haystack agent config file path found with value={}", configFilePath);
            }
        }

        return new ObjectMapper(new YAMLFactory()).readValue(new File(configFilePath), Config.class);
    }
}
