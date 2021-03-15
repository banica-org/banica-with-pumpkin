package com.market.banica.generator.service;

import com.market.MarketDataRequest;
import com.market.TickResponse;
import com.market.banica.generator.exception.NoSuchGoodException;
import io.grpc.StatusRuntimeException;
import io.grpc.netty.shaded.io.netty.util.internal.StringUtil;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MarketSubscriptionImpl implements Subscription<MarketDataRequest, TickResponse> {

    private final Logger LOGGER = LoggerFactory.getLogger(MarketSubscriptionImpl.class);
    private final Map<String, HashSet<StreamObserver<TickResponse>>> subscriptions = new ConcurrentHashMap<>();

    @Override
    public void subscribe(MarketDataRequest request, StreamObserver<TickResponse> responseObserver) {
        String goodName = getRequestGoodName(request);
        if (!StringUtil.isNullOrEmpty(goodName)) {
            LOGGER.debug("{} Requested for subscription for good: {}.", responseObserver, goodName);
            subscriptions.compute(goodName, (key, value) -> {
                value = (value == null ? new HashSet<>() : value);
                value.add(responseObserver);
                return value;
            });
        } else {
            LOGGER.warn("Illegal value {} for good by request: {}.", goodName, request);
        }
    }

    @Override
    public void unsubscribe(MarketDataRequest request, StreamObserver<TickResponse> responseObserver){
        String goodName = getRequestGoodName(request);
        if (!StringUtil.isNullOrEmpty(goodName)) {
            LOGGER.debug("Request to unsubscribe from {} for good: {}.", responseObserver, goodName);
            this.subscriptions.compute(goodName, (key, value) -> {
                if (this.subscriptions.get(goodName) == null) {
                    throw new NoSuchGoodException("No such good!");
                }
                if (value != null) {
                    if (value.size() == 1) {
                        return this.subscriptions.remove(goodName);
                    } else {
                        value.remove(responseObserver);
                    }
                    LOGGER.info("{} Unsubscribed successfully for good: {}.", responseObserver, goodName);
                }
                return value;
            });
        } else {
            LOGGER.warn("Illegal value {} for good by request: {}.", goodName, request);
        }
    }

    @Override
    public void notifySubscribers(TickResponse response) {
        String goodName = getTickResponseGoodName(response);
        if (!StringUtil.isNullOrEmpty(goodName)) {
            Set<StreamObserver<TickResponse>> subscribers = subscriptions.get(goodName);
            if (subscribers != null) {
                subscribers.forEach(subscriber -> {
                    try {
                        subscriber.onNext(response);
                    } catch (StatusRuntimeException e) {
                        LOGGER.debug("Subscriber {} unsubscribed.", subscriber);
                        subscribers.remove(subscriber);
                    }
                });
                LOGGER.debug("Notified subscribers successfully with: {}.", response);
            }
        } else {
            LOGGER.warn("Illegal value {} for good by response: {}.", goodName, response);
        }
    }

    @Override
    public String getRequestGoodName(MarketDataRequest request) {
        return request.getItemName();
    }

    @Override
    public String getTickResponseGoodName(TickResponse response) {
        return response.getItemName();
    }

    public HashSet<StreamObserver<TickResponse>> getSubscribers(String itemName) {
        return subscriptions.get(itemName);
    }
}
