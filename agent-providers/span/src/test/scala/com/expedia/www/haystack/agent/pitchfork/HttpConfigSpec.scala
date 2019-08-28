package com.expedia.www.haystack.agent.pitchfork

import java.io.IOException

import com.expedia.www.haystack.agent.pitchfork.service.config.HttpConfig
import com.typesafe.config.ConfigFactory
import org.scalatest.{FunSpec, Matchers}
import org.scalatest.easymock.EasyMockSugar

import scala.collection.JavaConverters._

class HttpConfigSpec extends FunSpec with Matchers with EasyMockSugar {
  describe("Http configuration provider") {
    it("should return gzip enabled as true if provided and its value is 'true'") {
      val config = ConfigFactory.parseMap(Map("port" -> 9115, "http.threads.min" -> 2, "http.threads.max" -> 4, "gzip.enabled" -> false).asJava)
      val httpConfig = HttpConfig.from(config)
      httpConfig.isGzipEnabled should equal (false)
    }
    it("should return gzip enabled as false if not provided")  {
      val config = ConfigFactory.parseMap(Map("port" -> 9115, "http.threads.min" -> 2, "http.threads.max" -> 4).asJava)
      val httpConfig = HttpConfig.from(config)
      httpConfig.isGzipEnabled should equal (true)
    }
    it("should return gzip buffer as 16Kb if not provided")  {
      val config = ConfigFactory.parseMap(Map("port" -> 9115, "http.threads.min" -> 2, "http.threads.max" -> 4).asJava)
      val httpConfig = HttpConfig.from(config)
      httpConfig.getGzipBufferSize should equal (16 * 1024)
    }
  }
}
