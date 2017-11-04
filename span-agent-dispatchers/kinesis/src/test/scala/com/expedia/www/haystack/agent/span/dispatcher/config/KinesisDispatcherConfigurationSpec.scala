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

package com.expedia.www.haystack.agent.span.dispatcher.config

import java.util

import org.scalatest.{FunSpec, Matchers}

class KinesisDispatcherConfigurationSpec extends FunSpec with Matchers {

  describe("Kinesis Dispatcher Configuration") {

    it("should take in a properties map and create the correct config object if all the required parameters are present") {

      val streamName = "test"
      val region = "us-west-2"
      val metricLevel = "detailed"
      val outstandingRecordsLimit: java.lang.Long = 1000l
      val props = new util.HashMap[String, Object]()
      props.put(KinesisDispatcherConfiguration.STREAM_NAME_KEY, streamName)
      props.put(KinesisDispatcherConfiguration.REGION_NAME_KEY, region)
      props.put(KinesisDispatcherConfiguration.OUTSTANDING_RECORDS_LIMIT_KEY, outstandingRecordsLimit)
      props.put(KinesisDispatcherConfiguration.METRIC_LEVEL_KEY, metricLevel)

      val config = new KinesisDispatcherConfiguration(props)
      config.getRegionName shouldEqual region
      config.getStreamName shouldEqual streamName
      config.getOutstandingRecordsLimit shouldEqual outstandingRecordsLimit
      config.getMetricLevel shouldEqual metricLevel
    }

    it("should take in a properties map and create the correct config object if all the required parameters are present and set default values for the optional parameters") {

      val streamName = "test"
      val region = "us-west-2"
      val props = new util.HashMap[String, Object]()
      props.put(KinesisDispatcherConfiguration.STREAM_NAME_KEY, streamName)
      props.put(KinesisDispatcherConfiguration.REGION_NAME_KEY, region)

      val config = new KinesisDispatcherConfiguration(props)
      config.getRegionName shouldEqual region
      config.getStreamName shouldEqual streamName
      config.getOutstandingRecordsLimit shouldEqual KinesisDispatcherConfiguration.DEFAULT_OUTSTANDING_RECORDS_LIMIT
      config.getMetricLevel shouldEqual KinesisDispatcherConfiguration.DEFAULT_METRIC_LEVEL
    }

    it("should throw an exception if stream name is not present in the properties map") {

      val region = "us-west-2"
      val props = new util.HashMap[String, Object]()
      props.put(KinesisDispatcherConfiguration.REGION_NAME_KEY, region)

      val exception = intercept[IllegalArgumentException] {
        new KinesisDispatcherConfiguration(props)
      }
      exception.getMessage shouldEqual s"Could not find key for ${KinesisDispatcherConfiguration.STREAM_NAME_KEY} in configuration"
    }


    it("should throw an exception if region name is not present in the properties map") {

      val streamName = "test"
      val props = new util.HashMap[String, Object]()
      props.put(KinesisDispatcherConfiguration.STREAM_NAME_KEY, streamName)

      val exception = intercept[IllegalArgumentException] {
        new KinesisDispatcherConfiguration(props)
      }
      exception.getMessage shouldEqual s"Could not find key for ${KinesisDispatcherConfiguration.REGION_NAME_KEY} in configuration"
    }
  }
}


