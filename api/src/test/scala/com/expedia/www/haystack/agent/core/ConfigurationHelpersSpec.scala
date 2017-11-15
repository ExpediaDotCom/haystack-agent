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

import com.expedia.www.haystack.agent.core.config.ConfigurationHelpers
import org.scalatest.{FunSpec, Matchers}


class ConfigurationHelpersSpec extends FunSpec with Matchers {

  describe("Configuration Helpers") {
    it("should override from env variables") {
      val configStr =
        """
          |agents {
          |  spans {
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
        """.stripMargin
      val cfg = ConfigurationHelpers.load(configStr)
      cfg.getString("agents.spans.k1") shouldEqual "v2"
      cfg.getInt("agents.spans.other") shouldEqual 100
    }
  }
}
