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

import com.codahale.metrics.{Meter, Timer}
import com.expedia.open.tracing.Span
import com.typesafe.config.ConfigFactory
import okhttp3._
import org.easymock.EasyMock
import org.scalatest.mock.EasyMockSugar
import org.scalatest.{FunSpec, Matchers}

class HttpDispatcherSpec extends FunSpec with Matchers with EasyMockSugar {
  describe("Http Span Dispatcher") {
    it("should dispatch span to http collector with success") {
      val dispatcher = new HttpDispatcher()
      val client = mock[OkHttpClient]
      val httpCall = mock[Call]
      val timer = mock[Timer]
      val dispatchFailure = mock[Meter]
      val responseBody = mock[ResponseBody]
      val httpResponse = new Response.Builder()
        .protocol(Protocol.HTTP_1_1)
        .request(new Request.Builder()
          .url("http://localhost:8080/span")
          .build())
        .code(200)
        .body(responseBody)
        .message("ok")
        .build
      val timerContext = mock[Timer.Context]

      dispatcher.client = client
      dispatcher.url = "http://localhost:8080/span"
      dispatcher.dispatchTimer = timer
      dispatcher.dispatchFailure = dispatchFailure

      val capturedRequest = EasyMock.newCapture[Request]()

      httpCall.execute().andReturn(httpResponse)
      timer.time().andReturn(timerContext)

      expecting {
        client.newCall(EasyMock.capture(capturedRequest)).andReturn(httpCall).once()
        timerContext.close().once()
      }

      whenExecuting(client, httpCall, timer, dispatchFailure, timerContext) {
        val span = Span.newBuilder().setTraceId("traceid").build()
        dispatcher.dispatch(span.getTraceId.getBytes("utf-8"), span.toByteArray)
        dispatcher.close()
      }
    }

    it("should should record an error if the request was not successful") {
      val dispatcher = new HttpDispatcher()
      val client = mock[OkHttpClient]
      val httpCall = mock[Call]
      val timer = mock[Timer]
      val dispatchFailure = mock[Meter]
      val responseBody = mock[ResponseBody]
      val httpResponse = new Response.Builder()
        .protocol(Protocol.HTTP_1_1)
        .request(new Request.Builder()
          .url("http://localhost:8080/span")
          .build())
        .code(500) // error status code
        .body(responseBody)
        .message("not ok")
        .build
      val timerContext = mock[Timer.Context]

      dispatcher.client = client
      dispatcher.url = "http://localhost:8080/span"
      dispatcher.dispatchTimer = timer
      dispatcher.dispatchFailure = dispatchFailure

      val capturedRequest = EasyMock.newCapture[Request]()

      httpCall.execute().andReturn(httpResponse)
      timer.time().andReturn(timerContext)

      expecting {
        client.newCall(EasyMock.capture(capturedRequest)).andReturn(httpCall).once()
        timerContext.close().once()
        dispatchFailure.mark().once()
      }

      whenExecuting(client, httpCall, timer, dispatchFailure, timerContext) {
        val span = Span.newBuilder().setTraceId("traceid").build()
        dispatcher.dispatch(span.getTraceId.getBytes("utf-8"), span.toByteArray)
        dispatcher.close()
      }
    }

    it("should should record an error if an exception is thrown") {
      val dispatcher = new HttpDispatcher()
      val client = mock[OkHttpClient]
      val httpCall = mock[Call]
      val timer = mock[Timer]
      val dispatchFailure = mock[Meter]
      val timerContext = mock[Timer.Context]

      dispatcher.client = client
      dispatcher.url = "http://localhost:8080/span"
      dispatcher.dispatchTimer = timer
      dispatcher.dispatchFailure = dispatchFailure

      val capturedRequest = EasyMock.newCapture[Request]()

      httpCall.execute().andStubThrow(new RuntimeException("error"))
      timer.time().andReturn(timerContext)

      expecting {
        client.newCall(EasyMock.capture(capturedRequest)).andReturn(httpCall).once()
        timerContext.close().once()
        dispatchFailure.mark().once()
      }

      whenExecuting(client, httpCall, timer, dispatchFailure, timerContext) {
        val span = Span.newBuilder().setTraceId("traceid").build()
        dispatcher.dispatch(span.getTraceId.getBytes("utf-8"), span.toByteArray)
        dispatcher.close()
      }
    }

    it("should fail to initialize http dispatcher if url property isn't present") {
      val dispatcher = new HttpDispatcher()
      val caught = intercept[Exception] {
        dispatcher.initialize(ConfigFactory.empty())
      }
      caught.getMessage shouldEqual "No configuration setting found for key 'url'"
    }

    it("should set configs from input") {
      val dispatcher = new HttpDispatcher()

      val config = ConfigFactory.parseString(
        """
          | url: "http://test:8080"
          | client.timeout.millis: 123
        """.stripMargin)

      dispatcher.initialize(config)

      dispatcher.url shouldBe "http://test:8080"
      dispatcher.client.callTimeoutMillis() shouldBe 123
    }
  }
}
