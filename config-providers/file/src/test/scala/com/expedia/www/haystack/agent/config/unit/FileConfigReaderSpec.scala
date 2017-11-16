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

package com.expedia.www.haystack.agent.config.unit

import com.expedia.www.haystack.agent.config.spi.FileConfigReader
import com.expedia.www.haystack.agent.core.config.ConfigurationHelpers
import com.typesafe.config.Config
import org.scalatest.{FunSpec, Matchers}

class FileConfigReaderSpec extends FunSpec with Matchers {

  describe("File ConfigReader spi") {
    it("should read the config file from system property and load the agent config") {
      val reader = new FileConfigReader()
      val confg = reader.read(new java.util.HashMap[String, String])
      reader.getName shouldEqual "file"
      validate(confg)
    }

    it("should read the config file in args map and load the agent config") {
      val reader = new FileConfigReader()
      val args = new java.util.HashMap[String, String]()
      args.put("--config-path", "src/test/resources/test.conf")
      val confg = reader.read(args)
      reader.getName shouldEqual "file"
      validate(confg)
    }
  }

  def validate(config: Config): Unit = {
    val agentsConfig = ConfigurationHelpers.readAgentConfigs(config)
    agentsConfig.size() shouldBe 2

    val spanAgentConfig = agentsConfig.get("spans")
    val configStr = spanAgentConfig.toString
    println(configStr)
    configStr shouldEqual "Config(SimpleConfigObject({\"dispatchers\":{\"kinesis\":{\"arn\":\"arn-1\",\"queueName\":\"myqueue\"}},\"enabled\":true,\"key1\":\"value1\",\"port\":8080}))"
    spanAgentConfig.getString("key1") shouldEqual "value1"
    spanAgentConfig.getInt("port") shouldBe 8080

    var dispatchersConfig = ConfigurationHelpers.readDispatchersConfig(spanAgentConfig)
    dispatchersConfig.size() shouldBe 1
    val kinesisDispatcher = dispatchersConfig.get("kinesis")
    kinesisDispatcher.getString("arn") shouldEqual "arn-1"
    kinesisDispatcher.getString("queueName") shouldEqual "myqueue"

    val blobsAgentConfig = agentsConfig.get("blobs")
    val blobConfStr = blobsAgentConfig.toString
    blobConfStr shouldEqual "Config(SimpleConfigObject({\"dispatchers\":{\"s3\":{\"iam\":\"iam-role\"}},\"enabled\":true,\"key2\":\"value2\",\"port\":80}))"
    blobsAgentConfig.getString("key2") shouldEqual "value2"
    blobsAgentConfig.getInt("port") shouldBe 80

    dispatchersConfig = ConfigurationHelpers.readDispatchersConfig(blobsAgentConfig)

    dispatchersConfig.size() shouldBe 1
    val s3Dispatcher = dispatchersConfig.get("s3")
    s3Dispatcher.getString("iam") shouldEqual "iam-role"
  }
}

