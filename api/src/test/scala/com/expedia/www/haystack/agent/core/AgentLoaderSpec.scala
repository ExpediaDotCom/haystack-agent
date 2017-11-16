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

package com.expedia.www.haystack.agent.core

import java.util
import java.util.ServiceConfigurationError

import com.expedia.www.haystack.agent.core.helpers.ReplacingClassLoader
import com.typesafe.config.ConfigFactory
import org.scalatest.{Entry, FunSpec, Matchers}

class AgentLoaderSpec extends FunSpec with Matchers {

  private val configServiceFile = "META-INF/services/com.expedia.www.haystack.agent.core.config.ConfigReader"
  private val agentServiceFile = "META-INF/services/com.expedia.www.haystack.agent.core.Agent"

  describe("Agent Loader") {
    it("should load the config spi") {
      val cl = new ReplacingClassLoader(getClass.getClassLoader, configServiceFile, "configProvider.txt")
      val cfg = new AgentLoader().loadConfig("file", new util.HashMap[String, String], cl)
      cfg.getConfig("agents") should not be null
      cfg.getConfig("agents").hasPath("spans") shouldBe true

      val agentConfig = cfg.getConfig("agents").getConfig("spans")

      agentConfig.getInt("port") shouldBe 8080
      agentConfig.getString("k1") shouldEqual "v1"

      agentConfig.getConfig("dispatchers") should not be null
      agentConfig.getConfig("dispatchers").hasPath("kinesis") shouldBe true
    }

    it("should fail to load the config spi with unknown name") {
      val cl = new ReplacingClassLoader(getClass.getClassLoader, configServiceFile, "configProvider.txt")
      val caught = intercept[ServiceConfigurationError] {
        new AgentLoader().loadConfig("http", new util.HashMap[String, String], cl)
      }
      caught.getMessage shouldEqual "Fail to load the config provider for type = http"
    }

    it("should load and initialize the agent using the config object") {
      val cl = new ReplacingClassLoader(getClass.getClassLoader, agentServiceFile, "singleAgentProvider.txt")

      val cfg = ConfigFactory.parseString(
        """
          |agents {
          |  spans {
          |    enabled = true
          |    k1 = "v1"
          |    port = 8080
          |
          |    dispatchers {
          |      kinesis {
          |        arn = "arn-1"
          |        queueName = "myqueue"
          |      }
          |    }
          |  }
          |}
        """.stripMargin)

      val runningAgents = new AgentLoader().loadAgents(cfg, cl)
      runningAgents.size() shouldBe 1
      runningAgents.get(0).close()
    }


    it("should not load the agent if disabled") {
      val cl = new ReplacingClassLoader(getClass.getClassLoader, agentServiceFile, "singleAgentProvider.txt")

      val cfg = ConfigFactory.parseString(
        """
          |agents {
          |  spans {
          |    enabled = false
          |    k1 = "v1"
          |    port = 8080
          |
          |    dispatchers {
          |      kinesis {
          |        arn = "arn-1"
          |        queueName = "myqueue"
          |      }
          |    }
          |  }
          |}
        """.stripMargin)

      val runningAgents = new AgentLoader().loadAgents(cfg, cl)
      runningAgents.size() shouldBe 0
    }

    it("should fail to load the agent for unidentified name") {
      val cl = new ReplacingClassLoader(getClass.getClassLoader, agentServiceFile, "singleAgentProvider.txt")

      val cfg = ConfigFactory.parseString(
        """
          |agents {
          |  blobs {
          |    enabled = true
          |    port = 8085
          |
          |    dispatchers {
          |      kafka {
          |        queueName = "myqueue"
          |      }
          |    }
          |  }
          |}
        """.stripMargin)

      val caught = intercept[ServiceConfigurationError] {
        new AgentLoader().loadAgents(cfg, cl)
      }
      caught.getMessage shouldEqual "Fail to load the agents with names=blobs"
    }

    it("should parse the null config reader args") {
      val result = AgentLoader.parseArgs(null)
      result.getKey shouldEqual "file"
      result.getValue shouldBe 'empty
    }

    it("should parse config reader args for file based reader") {
      val args = Array("--config-provider", "file", "--file-path", "/tmp/config.yaml")
      val result = AgentLoader.parseArgs(args)
      result.getKey shouldEqual "file"
      result.getValue should contain (Entry("--file-path", "/tmp/config.yaml"))
    }
  }
}