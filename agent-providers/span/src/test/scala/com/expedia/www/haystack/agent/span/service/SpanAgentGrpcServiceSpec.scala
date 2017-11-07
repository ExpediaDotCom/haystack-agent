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

package com.expedia.www.haystack.agent.span.service

import java.util.Collections

import com.expedia.open.tracing.Span
import com.expedia.open.tracing.agent.api.DispatchResult
import com.expedia.open.tracing.agent.api.DispatchResult.ResultCode
import com.expedia.www.haystack.agent.core.{Dispatcher, RateLimitException}
import io.grpc.stub.StreamObserver
import org.easymock.EasyMock
import org.scalatest.easymock.EasyMockSugar
import org.scalatest.{FunSpec, Matchers}

class SpanAgentGrpcServiceSpec extends FunSpec with Matchers with EasyMockSugar {

  describe("Span Agent Grpc service") {
    it("should dispatch the span successfully") {
      val span = Span.newBuilder().setTraceId("traceid").build()
      val dispatcher = mock[Dispatcher]
      val responseObserver = mock[StreamObserver[DispatchResult]]
      val service = new SpanAgentGrpcService(Collections.singletonList(dispatcher))

      val dispatchResult = EasyMock.newCapture[DispatchResult]()
      val capturedSpan = EasyMock.newCapture[Array[Byte]]()
      val capturedSpanPartitionKey = EasyMock.newCapture[Array[Byte]]()

      expecting {
        dispatcher.dispatch(EasyMock.capture(capturedSpanPartitionKey), EasyMock.capture(capturedSpan)).once()
        responseObserver.onNext(EasyMock.capture(dispatchResult)).once()
        responseObserver.onCompleted().once()
      }
      whenExecuting(dispatcher, responseObserver) {
        service.dispatch(span, responseObserver)
        dispatchResult.getValue.getCode shouldBe ResultCode.SUCCESS
        dispatchResult.getValue.getErrorMessage shouldBe ""
        capturedSpanPartitionKey.getValue shouldBe span.getTraceId.getBytes("utf-8")
        capturedSpan.getValue shouldBe span.toByteArray
      }
    }

    it("should dispatch the span with error if dispatcher fails") {
      val span = Span.newBuilder().setTraceId("traceid").build()
      val dispatcher = mock[Dispatcher]
      val responseObserver = mock[StreamObserver[DispatchResult]]
      val service = new SpanAgentGrpcService(Collections.singletonList(dispatcher))

      val dispatchResult = EasyMock.newCapture[DispatchResult]()
      val capturedSpan = EasyMock.newCapture[Array[Byte]]()
      val capturedSpanPartitionKey = EasyMock.newCapture[Array[Byte]]()

      expecting {
        dispatcher.getName.andReturn("test-dispatcher").anyTimes()
        dispatcher.dispatch(EasyMock.capture(capturedSpanPartitionKey), EasyMock.capture(capturedSpan)).andThrow(new RuntimeException("Fail to dispatch"))
        responseObserver.onNext(EasyMock.capture(dispatchResult)).once()
        responseObserver.onCompleted().once()
      }

      whenExecuting(dispatcher, responseObserver) {
        service.dispatch(span, responseObserver)
        dispatchResult.getValue.getCode shouldBe ResultCode.UNKNOWN_ERROR
        dispatchResult.getValue.getErrorMessage shouldEqual "Fail to dispatch the span record to the dispatchers=test-dispatcher"
        capturedSpanPartitionKey.getValue shouldBe span.getTraceId.getBytes("utf-8")
        capturedSpan.getValue shouldBe span.toByteArray
      }
    }

    it("should dispatch the span with rate limit error if dispatcher throws RateLimitException") {
      val span = Span.newBuilder().setTraceId("traceid").build()
      val dispatcher = mock[Dispatcher]
      val responseObserver = mock[StreamObserver[DispatchResult]]
      val service = new SpanAgentGrpcService(Collections.singletonList(dispatcher))

      val dispatchResult = EasyMock.newCapture[DispatchResult]()
      val capturedSpan = EasyMock.newCapture[Array[Byte]]()
      val capturedSpanPartitionKey = EasyMock.newCapture[Array[Byte]]()

      expecting {
        dispatcher.getName.andReturn("test-dispatcher").anyTimes()
        dispatcher.dispatch(EasyMock.capture(capturedSpanPartitionKey), EasyMock.capture(capturedSpan)).andThrow(new RateLimitException("Rate Limit Error!"))
        responseObserver.onNext(EasyMock.capture(dispatchResult)).once()
        responseObserver.onCompleted().once()
      }

      whenExecuting(dispatcher, responseObserver) {
        service.dispatch(span, responseObserver)
        dispatchResult.getValue.getCode shouldBe ResultCode.RATE_LIMIT_ERROR
        dispatchResult.getValue.getErrorMessage shouldEqual "Fail to dispatch the span record to the dispatchers=test-dispatcher"
        capturedSpanPartitionKey.getValue shouldBe span.getTraceId.getBytes("utf-8")
        capturedSpan.getValue shouldBe span.toByteArray
      }
    }

    it("should fail in constructing grpc service object if no dispatchers exist") {
      val caught = intercept[Exception] {
        new SpanAgentGrpcService(Collections.emptyList())
      }
      caught.getMessage shouldEqual "Dispatchers can't be empty"
    }
  }
}