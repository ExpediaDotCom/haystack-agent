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


package com.expedia.www.haystack.agent.offline.span.spi.helpers

import com.expedia.www.haystack.agent.core.Dispatcher
import com.typesafe.config.Config

class TestDispatcher extends Dispatcher {

  private var isInitialized = false

  /**
    * returns the unique name for this dispatcher
    *
    * @return
    */
  override def getName: String = "test-dispatcher"

  /**
    * dispatch the record to the sink
    *
    * @param partitionKey partitionKey if present, else send null
    * @param data         data bytes that need to be dispatched to the sink
    * @throws Exception throws exception if fails to dispatch
    */
  override def dispatch(partitionKey: Array[Byte], data: Array[Byte]) = ()

  /**
    * initializes the dispatcher for pushing span records to the sink
    *
    * @param conf
    */
  override def initialize(conf: Config): Unit = {
    isInitialized = true
    assert(conf != null && conf.getString("queueName") == "myqueue")
  }

  /**
    * close the dispatcher, this is called when the agent is shutting down.
    */
  override def close(): Unit = {
    assert(isInitialized, "Fail to close as the dispatcher isn't initialized yet")
  }
}
