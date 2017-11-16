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


import com.typesafe.config.Config;

public interface Agent extends AutoCloseable {

    /**
     * unique name of the agent, this is used to selectively load the agent by the name
     * @return unique name of the agent
     */
    String getName();

    /**
     * initialize the agent
     * @param config config object
     * @throws Exception throws an exception if fail to initialize
     */
    void initialize(final Config config) throws Exception;
}
