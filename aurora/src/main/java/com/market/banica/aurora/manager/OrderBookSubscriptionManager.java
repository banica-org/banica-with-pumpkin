package com.market.banica.aurora.manager;

import com.aurora.Aurora;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Component;

@Component
public class OrderBookSubscriptionManager {

    public void subscribeForOrderBookUpdate(Aurora.AuroraRequest request, StreamObserver<Aurora.AuroraResponse> responseObserver) {
    }
}
