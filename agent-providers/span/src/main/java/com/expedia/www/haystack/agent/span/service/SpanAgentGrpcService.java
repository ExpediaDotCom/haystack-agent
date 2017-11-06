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

package com.expedia.www.haystack.agent.span.service;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.expedia.open.tracing.Span;
import com.expedia.open.tracing.agent.api.DispatchResult;
import com.expedia.open.tracing.agent.api.SpanAgentGrpc;
import com.expedia.www.haystack.agent.core.span.Dispatcher;
import com.expedia.www.haystack.agent.core.metrics.SharedMetricRegistry;
import io.grpc.stub.StreamObserver;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class SpanAgentGrpcService extends SpanAgentGrpc.SpanAgentImplBase {

    private final Logger LOGGER = LoggerFactory.getLogger(SpanAgentGrpcService.class);

    private final List<Dispatcher> dispatchers;
    private final Timer dispatchTimer = SharedMetricRegistry.newTimer("span.agent.dispatch.timer");
    private final Meter dispatchFailureMeter = SharedMetricRegistry.newMeter("span.agent.dispatch.failures");

    public SpanAgentGrpcService(final List<Dispatcher> dispatchers) {
        Validate.notEmpty(dispatchers, "Dispatchers can't be empty");
        this.dispatchers = dispatchers;
    }

    @Override
    public void dispatch(final Span record, final StreamObserver<DispatchResult> responseObserver) {
        final DispatchResult.Builder result = DispatchResult.newBuilder().setCode(DispatchResult.ResultCode.SUCCESS);
        final StringBuilder failedDispatchers = new StringBuilder();

        final Timer.Context timer = dispatchTimer.time();

        for(final Dispatcher d : dispatchers) {
            try {
                d.dispatch(record);
            } catch (Exception ex) {
                dispatchFailureMeter.mark();
                LOGGER.error("Fail to dispatch the span record to the dispatcher with name={}", d.getName(), ex);
                failedDispatchers.append(d.getName()).append(',');
            }
        }

        if(failedDispatchers.length() > 0) {
            result.setCode(DispatchResult.ResultCode.ERROR);
            result.setErrorMessage("Fail to dispatch the span record to the dispatchers=" +
                    StringUtils.removeEnd(failedDispatchers.toString(), ","));
        }

        timer.close();

        responseObserver.onNext(result.build());
        responseObserver.onCompleted();
    }
}