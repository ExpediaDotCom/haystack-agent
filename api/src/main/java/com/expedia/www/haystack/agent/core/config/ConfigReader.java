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

public interface ConfigReader {
    /**
     * returns the unique name of this config reader, for e.g. 'file' if loading configuration
     * from file [ConfigFileReader], 'http' if reading the configuration from http endpoint
     * @return unique name for the config reader
     */
    String getName();

    /**
     * loads the config and returns the agent config object
     * @return Config object
     * @throws Exception
     */
    Config read() throws Exception;
}
