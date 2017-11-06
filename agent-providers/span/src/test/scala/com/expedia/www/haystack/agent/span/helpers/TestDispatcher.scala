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


package com.expedia.www.haystack.agent.span.helpers

import java.util

import com.expedia.open.tracing.Span
import com.expedia.www.haystack.agent.core.span.Dispatcher

class TestDispatcher extends Dispatcher {

  private var isInitialized = false

  /**
    * returns the unique name for this dispatcher
    *
    * @return
    */
  override def getName: String = "test-dispatcher"

  /**
    * dispatch the span record to the sink
    *
    * @param record proto span record
    * @throws Exception
    */
  override def dispatch(record: Span): Unit = ()

  /**
    * initializes the dispatcher for pushing span records to the sink
    *
    * @param conf
    */
  override def initialize(conf: util.Map[String, AnyRef]): Unit = {
    isInitialized = true
    assert(conf != null && conf.get("k1").toString.equals("v1"))
  }

  /**
    * close the dispatcher, this is called when the agent is shutting down.
    */
  override def close(): Unit = {
    assert(isInitialized, "Fail to close as the dispatcher isn't initialized yet")
  }
}
