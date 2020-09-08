package com.expedia.www.haystack.agent.span.service;

import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.health.v1.HealthGrpc;
import io.grpc.stub.StreamObserver;

public class SpanGrpcHealthService extends HealthGrpc.HealthImplBase {

    @Override
    public void check(HealthCheckRequest request, StreamObserver<HealthCheckResponse> responseObserver) {
        responseObserver.onNext(HealthCheckResponse.newBuilder().setStatus(HealthCheckResponse.ServingStatus.SERVING).build());
        responseObserver.onCompleted();
    }
}
