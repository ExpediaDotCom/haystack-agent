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

import java.util
import java.util.concurrent.{Future, TimeUnit}

import com.expedia.open.tracing.Span
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.kafka.clients.producer._
import org.easymock.EasyMock
import org.scalatest.{FunSpec, Matchers}
import org.scalatest.mock.EasyMockSugar

class KafkaDispatcherSpec extends FunSpec with Matchers with EasyMockSugar {
  describe("Kafka Span Dispatcher") {
    it("should dispatch span to kafka with success") {
      val dispatcher = new KafkaDispatcher()
      val producer = mock[KafkaProducer[Array[Byte], Array[Byte]]]
      val future = mock[Future[RecordMetadata]]
      dispatcher.setKafkaProducer(producer)
      dispatcher.setTopic("mytopic")

      val capturedProducerRecord = EasyMock.newCapture[ProducerRecord[Array[Byte], Array[Byte]]]()
      expecting {
        producer.send(EasyMock.capture(capturedProducerRecord), EasyMock.anyObject(classOf[Callback])).andReturn(future).once()
        producer.flush().once()
        producer.close(10, TimeUnit.SECONDS).once
      }

      whenExecuting(producer, future) {
        val span = Span.newBuilder().setTraceId("traceid").build()
        dispatcher.dispatch(span.getTraceId.getBytes("utf-8"), span.toByteArray)
        val producerRecord = capturedProducerRecord.getValue
        producerRecord.topic() shouldEqual "mytopic"
        new String(producerRecord.key()) shouldEqual "traceid"
        producerRecord.value() shouldBe span.toByteArray

        // close the dispatcher and verify if it is flushed and closed
        dispatcher.close()
      }
    }

    it("should fail to initialize kafka if bootstrap.servers property isn't present") {
      val kafka = new KafkaDispatcher()
      val caught = intercept[Exception] {
        kafka.initialize(ConfigFactory.empty())
      }
      caught.getMessage shouldEqual "No configuration setting found for key 'bootstrap'"
    }

    it("should fail to initialize kafka if producer.topic property isn't present") {
      val kafka = new KafkaDispatcher()

      val config = ConfigFactory.parseString(
        """
          | bootstrap.servers: "localhost:9092"
        """.stripMargin)
      val caught = intercept[Exception] {
        kafka.initialize(config)
      }
      caught.getMessage shouldEqual "No configuration setting found for key 'producerTopic'"
    }
  }
}