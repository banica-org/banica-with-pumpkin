package com.market.banica.generator.service;

import com.aurora.Aurora;
import com.market.TickResponse;
import io.grpc.stub.StreamObserver;

public interface SubscriptionManager {

    void subscribe(Aurora.AuroraRequest request, StreamObserver<Aurora.AuroraResponse> responseObserver);

    void notifySubscribers(TickResponse response);

}
