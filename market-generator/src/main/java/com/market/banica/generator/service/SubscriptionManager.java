package com.market.banica.generator.service;

import com.market.MarketDataRequest;
import com.market.TickResponse;
import io.grpc.stub.StreamObserver;

public interface SubscriptionManager {

    void subscribe(MarketDataRequest request, StreamObserver<TickResponse> responseObserver);

    void notifySubscribers(TickResponse response);

}
