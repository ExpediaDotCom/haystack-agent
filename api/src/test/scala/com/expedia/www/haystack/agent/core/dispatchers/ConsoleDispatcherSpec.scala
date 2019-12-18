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

package com.expedia.www.haystack.agent.core.dispatchers

import java.io.{ByteArrayOutputStream, PrintStream}

import com.expedia.open.tracing.Span
import com.expedia.www.haystack.agent.core.dispatchers.ConsoleDispatcher
import org.scalatest.{FunSpec, Matchers}

class ConsoleDispatcherSpec extends FunSpec with Matchers {
  describe("Console Dispatcher") {
    it("should dispatch span to console collector with success") {
      val outContent: ByteArrayOutputStream = new ByteArrayOutputStream
      val sOut = System.out
      System.setOut(new PrintStream(outContent))

      val dispatcher = new ConsoleDispatcher()

      val span = Span.newBuilder().setTraceId("traceid").build()
      dispatcher.dispatch(span.getTraceId.getBytes("utf-8"), span.toByteArray)
      dispatcher.close()
      System.setOut(sOut)

      // There are platform specific issues around the println that we prefer to keep this
      // test simple and assert that the traceId is part of the printed content.
      outContent.toString() should include ("traceid")
    }
  }
}
