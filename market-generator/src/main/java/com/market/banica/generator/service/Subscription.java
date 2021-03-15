package com.market.banica.generator.service;

import io.grpc.stub.StreamObserver;

public interface Subscription<T, S> {

    void subscribe(T request, StreamObserver<S> responseObserver);

    void unsubscribe(T request, StreamObserver<S> responseObserver);

    void notifySubscribers(S response);

    String getRequestGoodName(T request);

    String getTickResponseGoodName(S response);
}
