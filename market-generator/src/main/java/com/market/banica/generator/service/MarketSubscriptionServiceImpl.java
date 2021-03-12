package com.market.banica.generator.service;

import com.market.MarketDataRequest;
import com.market.TickResponse;
import io.grpc.stub.StreamObserver;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MarketSubscriptionServiceImpl implements SubscriptionService<MarketDataRequest, TickResponse>{

    private final Map<String, Set<StreamObserver<TickResponse>>> subscriptions = new ConcurrentHashMap<>();

    @Override
    public void subscribe(MarketDataRequest request, StreamObserver<TickResponse> responseObserver) {

    }

    @Override
    public void unsubscribe(MarketDataRequest request, StreamObserver<TickResponse> responseObserver) {

    }

    @Override
    public void notifySubscribers(TickResponse response) {

    }

    @Override
    public String getRequestItemName(MarketDataRequest request) {
        return request.getItemName();
    }

    @Override
    public String getTickResponseItemName(TickResponse response) {
        return response.getItemName();
    }

    public Set<StreamObserver<TickResponse>> getSubscribers(String itemName){
        return subscriptions.get(itemName);
    }
}
