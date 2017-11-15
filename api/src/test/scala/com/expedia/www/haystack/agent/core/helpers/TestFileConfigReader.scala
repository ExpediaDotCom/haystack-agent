package com.expedia.www.haystack.agent.core.helpers

import java.util

import com.expedia.www.haystack.agent.core.config.ConfigReader
import com.typesafe.config.{Config, ConfigFactory}

class TestFileConfigReader extends ConfigReader {
  override def getName: String = "file"

  override def read(args: util.Map[String, String]): Config = {
    ConfigFactory.parseString(
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
      """.stripMargin)
  }
}