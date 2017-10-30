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

import com.expedia.www.haystack.agent.config.Config;
import com.expedia.www.haystack.agent.config.ConfigReader;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.commons.lang3.StringUtils;

import java.io.File;

/**
 * loads the config for all agents from file. It uses system property, environment variable and default config file
 * in the given order to find out config file path.
 */
public class FileConfigReader implements ConfigReader {
    private final static String DEFAULT_CONFIG_FILE_PATH = "agent-config.yaml";
    private final static String CONFIG_FILE_PROP_NAME = "HAYSTACK_CONFIG_FILE_PATH";

    @Override
    public String getName() {
        return "file";
    }

    @Override
    public Config read() throws Exception {
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

        // fallback to the default config path
        String configFilePath = System.getProperty(CONFIG_FILE_PROP_NAME);
        if (StringUtils.isEmpty(configFilePath)) {
            configFilePath = System.getenv(CONFIG_FILE_PROP_NAME);
            if (StringUtils.isEmpty(configFilePath)) {
                configFilePath = DEFAULT_CONFIG_FILE_PATH;
            }
        }
        return mapper.readValue(new File(configFilePath), Config.class);
    }
}
