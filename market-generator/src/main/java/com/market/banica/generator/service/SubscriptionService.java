package com.market.banica.generator.service;

import io.grpc.stub.StreamObserver;

public interface SubscriptionService<T, S> {

    void subscribe(T request, StreamObserver<S> responseObserver);

    void unsubscribe(T request, StreamObserver<S> responseObserver);

    void notifySubscribers(S response);

    String getRequestItemName(T request);

    String getTickResponseItemName(S response);
}
