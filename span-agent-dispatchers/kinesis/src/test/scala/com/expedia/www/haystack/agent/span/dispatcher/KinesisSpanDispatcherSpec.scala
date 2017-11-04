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

package com.expedia.www.haystack.agent.span.dispatcher

import java.util

import org.scalatest.{FunSpec, Matchers}

class KinesisSpanDispatcherSpec extends FunSpec with Matchers {

  private val STREAM_NAME_KEY = "StreamName"
  /**
    * Region name to which instance of Kinesis trace will be send.
    */

  private val REGION_NAME_KEY = "Region"
  /**
    * metric level for reporting cloudwatch metrics
    */

  private val METRIC_LEVEL_KEY = "MetricLevel"

  /**
    * The number of outstanding records after which the dispatcher starts dropping spans
    */
  private val OUTSTANDING_RECORDS_LIMIT_KEY = "OutstandingRecordsLimit"

  protected val DEFAULT_OUTSTANDING_RECORDS_LIMIT = 10000L


  val streamName = "test"
  val region = "us-west-2"
  val metricLevel = "detailed"
  val outstandingRecordsLimit: java.lang.Long = 1000l

  describe("Kinesis Dispatcher") {

    it("given a config object to be initialized it should be able to fetch the stream name and outstandin records limit") {

      val props = new util.HashMap[String, Object]()
      props.put(STREAM_NAME_KEY, streamName)
      props.put(REGION_NAME_KEY, region)
      props.put(OUTSTANDING_RECORDS_LIMIT_KEY, outstandingRecordsLimit)
      props.put(METRIC_LEVEL_KEY, metricLevel)

      val kinesisSpanDispatcher = new KinesisSpanDispatcher()

      kinesisSpanDispatcher.getAndRemoveStreamNameKey(props) shouldEqual streamName
      kinesisSpanDispatcher.getAndRemoveOutstandingRecordLimitKey(props) shouldEqual outstandingRecordsLimit
    }

    it("should be able to build the kinesis producer configuration using the same keys as in the config passed") {

      val streamName = "test"
      val region = "us-west-2"
      val metricLevel = "detailed"
      val props = new util.HashMap[String, Object]()
      props.put(STREAM_NAME_KEY, streamName)
      props.put(REGION_NAME_KEY, region)
      props.put(OUTSTANDING_RECORDS_LIMIT_KEY, outstandingRecordsLimit)
      props.put(METRIC_LEVEL_KEY, metricLevel)

      val kinesisSpanDispatcher = new KinesisSpanDispatcher()
      val config = kinesisSpanDispatcher.buildKinesisProducerConfiguration(props)

      config.getRegion shouldEqual region
      config.getMetricsLevel shouldEqual metricLevel
    }
  }
}



