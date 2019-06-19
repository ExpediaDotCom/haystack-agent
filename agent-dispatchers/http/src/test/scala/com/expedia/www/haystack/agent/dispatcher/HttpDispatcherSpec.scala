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

import com.codahale.metrics.Timer
import com.typesafe.config.ConfigFactory
import okhttp3.{Call, OkHttpClient, Request}
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
      val timerContext = mock[Timer.Context]

      dispatcher.client = client
      dispatcher.url = "http://localhost:8080/span"
      dispatcher.dispatchTimer = timer

      val capturedRequest = EasyMock.newCapture[Request]()
      expecting {
        client.newCall(EasyMock.capture(capturedRequest)).andReturn(httpCall).once()
        timer.time().andReturn(timerContext)
      }
    }

    it("should fail to initialize http dispatcher if url property isn't present") {
      val dispatcher = new HttpDispatcher()
      val caught = intercept[Exception] {
        dispatcher.initialize(ConfigFactory.empty())
      }
      caught.getMessage shouldEqual "No configuration setting found for key 'url'"
    }
  }
}
