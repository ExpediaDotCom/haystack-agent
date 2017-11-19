package com.expedia.www.haystack.agent.core

import com.expedia.www.haystack.agent.core.metrics.SharedMetricRegistry
import org.scalatest.{FunSpec, Matchers}

class SharedMetricRegistrySpec extends FunSpec with Matchers {

  describe("SharedMetricRegistry") {
    it("should build the right metric name if agentName is not empty") {
      SharedMetricRegistry.buildMetricName("spans", "my.timer") shouldEqual "spans.my.timer"
    }

    it("should build the right metric name if agentName is empty") {
      SharedMetricRegistry.buildMetricName("", "my.timer") shouldEqual "my.timer"
    }
  }
}
