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
import java.util.Optional

import com.expedia.www.haystack.agent.core.config.ConfigurationHelpers
import org.scalatest.{FunSpec, Matchers}


class ConfigurationHelpersSpec extends FunSpec with Matchers {


  private val STREAM_NAME_KEY = "StreamName"
  /**
    * Region name to which instance of Kinesis trace will be send.
    */
  private val REGION_NAME_KEY = "RegionName"
  /**
    * metric level for reporting cloudwatch metrics
    */

  private val METRIC_LEVEL_KEY = "MetricLevel"
  /**
    * The number of outstanding records after which the dispatcher starts dropping spans
    */
  private val OUTSTANDING_RECORDS_LIMIT_KEY = "OutstandingRecordsLimit"

  private val DEFAULT_OUTSTANDING_RECORDS_LIMIT: java.lang.Long = 10000L

  describe("Configuration Helpers") {

    it("Generate Properties should take in a Map and generate a properties object with same objects as keys") {

      val streamName = "test"
      val region = "us-west-2"
      val metricLevel = "detailed"
      val outstandingRecordsLimit: java.lang.Long = 1000l
      val config = new util.HashMap[String, Object]()
      config.put(STREAM_NAME_KEY, streamName)
      config.put(REGION_NAME_KEY, region)
      config.put(OUTSTANDING_RECORDS_LIMIT_KEY, outstandingRecordsLimit)
      config.put(METRIC_LEVEL_KEY, metricLevel)

      val props = ConfigurationHelpers.generatePropertiesFromMap(config)

      props.getProperty(STREAM_NAME_KEY) shouldEqual streamName
      props.getProperty(REGION_NAME_KEY) shouldEqual region
      props.getProperty(METRIC_LEVEL_KEY) shouldEqual metricLevel
    }

    it("getPropertyAsType should take in a Map and return the property requested in the correct type as requested") {

      val streamName = "test"
      val region = "us-west-2"
      val metricLevel = "detailed"
      val outstandingRecordsLimit: java.lang.Long = 1000l
      val config = new util.HashMap[String, Object]()
      config.put(STREAM_NAME_KEY, streamName)
      config.put(REGION_NAME_KEY, region)
      config.put(OUTSTANDING_RECORDS_LIMIT_KEY, outstandingRecordsLimit)
      config.put(METRIC_LEVEL_KEY, metricLevel)

      val resolvedStreamName = ConfigurationHelpers.getPropertyAsType(config, STREAM_NAME_KEY, classOf[String], Optional.empty())
      val resolvedOutstandingRecordsLimit = ConfigurationHelpers.getPropertyAsType(config, OUTSTANDING_RECORDS_LIMIT_KEY, classOf[java.lang.Long], Optional.empty())
      resolvedStreamName shouldEqual streamName
      resolvedOutstandingRecordsLimit shouldEqual outstandingRecordsLimit
    }

    it("getPropertyAsType should return the default in case the key does not exist in the map if the default is set else throw an exception") {

      val region = "us-west-2"
      val metricLevel = "detailed"
      val config = new util.HashMap[String, Object]()
      config.put(REGION_NAME_KEY, region)
      config.put(METRIC_LEVEL_KEY, metricLevel)

      val exception = intercept[IllegalArgumentException] {
        ConfigurationHelpers.getPropertyAsType(config, STREAM_NAME_KEY, classOf[String], Optional.empty())
      }
      exception.getMessage shouldEqual s"Could not find key for $STREAM_NAME_KEY in configuration"

      val resolvedOutstandingRecordsLimit = ConfigurationHelpers.getPropertyAsType(config, OUTSTANDING_RECORDS_LIMIT_KEY, classOf[java.lang.Long], Optional.of(DEFAULT_OUTSTANDING_RECORDS_LIMIT))
      resolvedOutstandingRecordsLimit shouldEqual DEFAULT_OUTSTANDING_RECORDS_LIMIT
    }
  }
}
