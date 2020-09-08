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

import com.expedia.open.tracing.Span
import com.expedia.www.haystack.agent.span.enricher.Enricher
import com.typesafe.config.ConfigFactory
import org.scalatest.easymock.EasyMockSugar
import org.scalatest.{FunSpec, Matchers}

import scala.collection.JavaConversions._

class DummyEnricher extends Enricher {
  override def apply(span: Span.Builder): Unit = ()
}

class SpanAgentSpec extends FunSpec with Matchers with EasyMockSugar {

  private val dispatcherLoadFile = "META-INF/services/com.expedia.www.haystack.agent.core.Dispatcher"

  describe("Span Agent") {
    it("should return the 'spans' as agent name") {
      new SpanAgent().getName shouldEqual "spans"
    }

    it("should load the dispatchers from the config") {
      val agent = new SpanAgent()
      val cfg = ConfigFactory.parseString(
        """
          |    k1 = "v1"
          |    port = 8080
          |
          |    dispatchers {
          |      test-dispatcher {
          |        queueName = "myqueue"
          |      }
          |      test-dispatcher-empty-config {
          |      }
          |    }
        """.stripMargin)

      val cl = new ReplacingClassLoader(getClass.getClassLoader, dispatcherLoadFile, "dispatcherProvider.txt")
      val dispatchers = agent.loadAndInitializeDispatchers(cfg, cl, "spans")
      dispatchers.size() shouldBe 2
      dispatchers.map(_.getName) should contain allOf ("test-dispatcher", "test-dispatcher-empty-config")
      dispatchers.foreach(_.close())
    }

    it("should not load an unknown dispatcher") {
      val agent = new SpanAgent()
      val cfg = ConfigFactory.parseString(
        """
          |    k1 = "v1"
          |    port = 8080
          |
          |    dispatchers {
          |      test-dispatcher {
          |        queueName = "myqueue"
          |      }
          |      test-dispatcher-3 {
          |         enabled = false
          |      }
          |    }
        """.stripMargin)

      val cl = new ReplacingClassLoader(getClass.getClassLoader, dispatcherLoadFile, "dispatcherProvider.txt")
      val dispatchers = agent.loadAndInitializeDispatchers(cfg, cl, "spans")
      dispatchers.size() shouldBe 1
      dispatchers.head.getName shouldBe "test-dispatcher"
      dispatchers.foreach(_.close())
    }

    it("should not load a 'disabled' dispatcher") {
      val agent = new SpanAgent()
      val cfg = ConfigFactory.parseString(
        """
          |    k1 = "v1"
          |    port = 8080
          |
          |    dispatchers {
          |      test-dispatcher {
          |        queueName = "myqueue"
          |      }
          |      test-dispatcher-2 {
          |         enabled = false
          |      }
          |    }
        """.stripMargin)

      val cl = new ReplacingClassLoader(getClass.getClassLoader, dispatcherLoadFile, "dispatcherProvider.txt")
      val dispatchers = agent.loadAndInitializeDispatchers(cfg, cl, "spans")
      dispatchers.size() shouldBe 1
      dispatchers.head.getName shouldBe "test-dispatcher"
      dispatchers.foreach(_.close())
    }


    it("initialization should fail if no dispatchers exist") {
      val agent = new SpanAgent()
      val cfg = ConfigFactory.parseString(
        """
          |    k1 = "v1"
          |    port = 8080
          |
          |    dispatchers {
          |      test-dispatcher {
          |        queueName = "myqueue"
          |      }
          |    }
        """.stripMargin)

      val caught = intercept[Exception] {
        agent.loadAndInitializeDispatchers(cfg, getClass.getClassLoader, "spans")
      }

      caught.getMessage shouldEqual "Spans agent dispatchers can't be an empty set"
    }

    it ("should load enrichers") {
      val agent = new SpanAgent()
      val cfg = ConfigFactory.parseString(
        """
          |    k1 = "v1"
          |    port = 8080
          |
          |    enrichers = [
          |       "com.expedia.www.haystack.agent.span.spi.DummyEnricher"
          |    ]
        """.stripMargin)
      val enrichers = agent.loadSpanEnrichers(cfg)
      enrichers.length shouldBe 1
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
