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

package com.expedia.www.haystack.agent.core.dispatchers;

import com.expedia.www.haystack.agent.core.Dispatcher;
import com.typesafe.config.Config;

/**
 * Dispatches the data into the console.
 */
public class ConsoleDispatcher implements Dispatcher {
    @Override
    public String getName() {
        return "console";
    }

    @Override
    @SuppressWarnings("PMD.SystemPrintln")
    public void dispatch(final byte[] partitionKey, final byte[] data) {
        System.out.println(new String(data));
    }

    @Override
    public void initialize(final Config conf) {
    }

    @Override
    public void close() {
    }
}