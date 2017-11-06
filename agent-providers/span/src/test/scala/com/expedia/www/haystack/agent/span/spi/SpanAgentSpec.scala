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


package com.expedia.www.haystack.agent.span.spi

import java.io.IOException
import java.net.URL
import java.util

import com.expedia.www.haystack.agent.core.config.AgentConfig
import org.scalatest.{FunSpec, Matchers}
import org.scalatest.easymock.EasyMockSugar
import scala.collection.JavaConversions._

class SpanAgentSpec extends FunSpec with Matchers with EasyMockSugar {
  private val dispatcherLoadFile = "META-INF/services/com.expedia.www.haystack.agent.core.span.Dispatcher"

  describe("Span Agent") {
    it("should return the 'spans' as agent name") {
      new SpanAgent().getName shouldEqual "spans"
    }

    it("should load the dispatchers from the config") {
      val agent = new SpanAgent()
      val agentConfig = new AgentConfig
      agentConfig.setName("spans")
      val dispatchersCfgMap = new util.HashMap[String, util.Map[String, Object]]()
      val testDispatcherConfig = new util.HashMap[String, Object]()
      testDispatcherConfig.put("k1", "v1")
      dispatchersCfgMap.put("test-dispatcher", testDispatcherConfig)
      agentConfig.setDispatchers(dispatchersCfgMap)

      val cl = new ReplacingClassLoader(getClass.getClassLoader, dispatcherLoadFile, "dispatcherProvider.txt")
      val dispatchers = agent.getAndInitializeDispatchers(agentConfig, cl)
      dispatchers.size() shouldBe 1
      dispatchers.head.close()
    }
  }

  class ReplacingClassLoader(val parent: ClassLoader, val resource: String, val replacement: String) extends ClassLoader(parent) {
    override def getResource(name: String): URL = {
      if (resource == name) {
        return getParent.getResource(replacement)
      }
      super.getResource(name)
    }

    @throws[IOException]
    override def getResources(name: String): util.Enumeration[URL] = {
      if (resource == name) {
        return getParent.getResources(replacement)
      }
      super.getResources(name)
    }
  }
}
