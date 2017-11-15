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

package com.expedia.www.haystack.agent.dispatcher

import java.nio.ByteBuffer
import java.util

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.auth.profile.internal.securitytoken.STSProfileCredentialsServiceProvider
import com.amazonaws.services.kinesis.producer.{KinesisProducer, UserRecordResult}
import com.expedia.open.tracing.Span
import com.expedia.www.haystack.agent.dispatcher.KinesisDispatcher._
import com.google.common.util.concurrent.ListenableFuture
import org.easymock.EasyMock
import org.scalatest.easymock.EasyMockSugar
import org.scalatest.{FunSpec, Matchers}

class KinesisSpanDispatcherSpec extends FunSpec with Matchers with EasyMockSugar {

  private val REGION_NAME_KEY = "Region"
  private val METRIC_LEVEL_KEY = "MetricsLevel"
  protected val DEFAULT_OUTSTANDING_RECORDS_LIMIT = 10000L


  val streamName = "test"
  val region = "us-west-2"
  val metricLevel = "detailed"
  val outstandingRecordsLimit: Integer = 1000

  describe("Kinesis Dispatcher") {

    it("given a config object to be initialized it should be able to fetch the stream name and outstandin records limit") {
      val props = new util.HashMap[String, String]()
      props.put(STREAM_NAME_KEY, streamName)
      props.put(REGION_NAME_KEY, region)
      props.put(OUTSTANDING_RECORD_LIMIT_KEY, outstandingRecordsLimit.toString)
      props.put(METRIC_LEVEL_KEY, metricLevel)

      val dispatcher = new KinesisDispatcher()

      dispatcher.getAndRemoveStreamNameKey(props) shouldEqual streamName
      dispatcher.getAndRemoveOutstandingRecordLimitKey(props) shouldEqual outstandingRecordsLimit
      props.containsKey(KinesisDispatcher.STREAM_NAME_KEY) shouldBe false
      props.containsKey(KinesisDispatcher.OUTSTANDING_RECORD_LIMIT_KEY) shouldBe false
    }

    it("should be able to build the kinesis producer configuration using the same keys as in the config passed") {
      val streamName = "test"
      val region = "us-west-2"
      val metricLevel = "detailed"
      val props = new util.HashMap[String, String]()
      props.put(STREAM_NAME_KEY, streamName)
      props.put(REGION_NAME_KEY, region)
      props.put(OUTSTANDING_RECORD_LIMIT_KEY, outstandingRecordsLimit.toString)
      props.put(METRIC_LEVEL_KEY, metricLevel)

      val dispatcher = new KinesisDispatcher()
      val config = dispatcher.buildKinesisProducerConfiguration(props)

      config.getRegion shouldEqual region
      config.getMetricsLevel shouldEqual metricLevel
      config.getCredentialsProvider shouldBe  DefaultAWSCredentialsProviderChain.getInstance()
    }


    it("should be able to build the kinesis producer configuration with sts role arn") {
      val streamName = "test"
      val region = "us-west-2"
      val props = new util.HashMap[String, String]()
      props.put(STREAM_NAME_KEY, streamName)
      props.put(REGION_NAME_KEY, region)
      props.put(OUTSTANDING_RECORD_LIMIT_KEY, outstandingRecordsLimit.toString)
      props.put(STS_ROLE_ARN, "some-arn")

      val dispatcher = new KinesisDispatcher()
      val config = dispatcher.buildKinesisProducerConfiguration(props)

      config.getRegion shouldEqual region
      config.getCredentialsProvider.getClass shouldBe classOf[STSProfileCredentialsServiceProvider]
    }

    it("should dispatch span to kinesis") {
      val dispatcher = new KinesisDispatcher()
      val kinesisProducer = mock[KinesisProducer]
      val responseFuture = mock[ListenableFuture[UserRecordResult]]

      dispatcher.setKinesisProducer(kinesisProducer)
      dispatcher.setStreamName("mystream")
      dispatcher.setOutstandingRecordsLimit(1000)

      val span = Span.newBuilder().setTraceId("traceid").build()

      expecting {
        kinesisProducer.getOutstandingRecordsCount.andReturn(10).once
        kinesisProducer.addUserRecord("mystream", "traceid", ByteBuffer.wrap(span.toByteArray)).andReturn(responseFuture).once()
        kinesisProducer.flushSync().once()
        kinesisProducer.destroy().once()
        responseFuture.addListener(EasyMock.anyObject(), EasyMock.anyObject())
      }

      whenExecuting(kinesisProducer, responseFuture) {
        dispatcher.dispatch(span.getTraceId.getBytes("utf-8"), span.toByteArray)
        dispatcher.close()
      }
    }

    it("should fail dispatch span to kinesis with outstanding limit error") {
      val dispatcher = new KinesisDispatcher()
      val kinesisProducer = mock[KinesisProducer]

      dispatcher.setKinesisProducer(kinesisProducer)
      dispatcher.setStreamName("mystream")
      dispatcher.setOutstandingRecordsLimit(1000)

      val span = Span.newBuilder().setTraceId("traceid").build()

      expecting {
        kinesisProducer.getOutstandingRecordsCount.andReturn(1001).anyTimes()
      }

      whenExecuting(kinesisProducer) {
        val caught = intercept[Exception] {
          dispatcher.dispatch(span.getTraceId.getBytes("utf-8"), span.toByteArray)
        }

        caught.getMessage shouldEqual "fail to dispatch to kinesis due to rate limit, outstanding records: 1001"
      }
    }
  }
}



