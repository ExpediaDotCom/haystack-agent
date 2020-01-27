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

import java.io.{ByteArrayOutputStream, PrintStream}

import com.expedia.open.tracing.{Span, Tag}
import org.scalatest.{FunSpec, Matchers}

class LoggerDispatcherSpec extends FunSpec with Matchers {
  describe("Logger Dispatcher") {
    it("should dispatch span to stdOut") {
      val sOut = System.out
      try {
        val outContent: ByteArrayOutputStream = new ByteArrayOutputStream
        System.setOut(new PrintStream(outContent))

        val dispatcher = new LoggerDispatcher()

        val span = Span.newBuilder()
          .setTraceId("7f46165474d11ee5836777d85df2cdab")
          .setSpanId("30d1aee5836717f0")
          .addTags(Tag.newBuilder().setKey("http.path").setType(Tag.TagType.STRING).setVStr("/my-path").build())
          .addTags(Tag.newBuilder().setKey("sql.affected_row").setType(Tag.TagType.LONG).setVLong(1).build())
          .build()

        dispatcher.dispatch(span.getTraceId.getBytes("utf-8"), span.toByteArray)
        dispatcher.close()
        outContent.toString().contains("{\"traceId\":\"7f46165474d11ee5836777d85df2cdab\",\"spanId\":\"30d1aee5836717f0\",\"startTime\":0,\"tags\":[{\"key\":\"http.path\",\"type\":\"STRING\",\"vString\":\"/my-path\"}{\"key\":\"sql.affected_row\",\"type\":\"LONG\",\"vLong\":1}]}")
      } finally {
        /* Makes sure the result of the test will be reported to stdOut */
        System.setOut(sOut)
      }
    }

    it("should fail to dispatch invalid data into stdOut") {
      val sOut = System.out
      try {
        val outContent: ByteArrayOutputStream = new ByteArrayOutputStream
        System.setOut(new PrintStream(outContent))

        val dispatcher = new LoggerDispatcher()

        dispatcher.dispatch("none".getBytes(), "abc".getBytes())
        dispatcher.close()

        outContent.toString().contains("failed to parse span:") shouldBe true
      } finally {
        /* Makes sure the result of the test will be reported to stdOut */
        System.setOut(sOut)
      }
    }
  }
}
