package com.market.banica.generator.service;

import com.market.MarketDataRequest;
import com.market.TickResponse;
import com.market.banica.generator.exception.NotFoundException;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MarketSubscriptionManager implements SubscriptionManager<MarketDataRequest, TickResponse> {


    private final Logger LOGGER = LoggerFactory.getLogger(MarketSubscriptionManager.class);
    private final Map<String, HashSet<StreamObserver<TickResponse>>> subscriptions = new ConcurrentHashMap<>();

    @Override
    public void subscribe(MarketDataRequest request, StreamObserver<TickResponse> responseObserver) {
        String goodName = getRequestGoodName(request);
        if (validateStringData(goodName)) {
            LOGGER.debug("{} Requested for subscription for good: {}.", responseObserver, goodName);
            addSubscriber(responseObserver, goodName);
        } else {
            LOGGER.warn("Illegal value {} for good by request: {}.", goodName, request);
            throw new NotFoundException("Illegal good value!");
        }
    }

    @Override
    public void unsubscribe(MarketDataRequest request, StreamObserver<TickResponse> responseObserver) {
        String goodName = getRequestGoodName(request);
        if (validateStringData(goodName)) {
            LOGGER.debug("Request to unsubscribe from {} for good: {}.", responseObserver, goodName);
            removeSubscriber(responseObserver, goodName);
        } else {
            LOGGER.warn("Illegal value {} for good by request: {}.", goodName, request);
            throw new NotFoundException("Illegal good value!");
        }
    }

    @Override
    public void notifySubscribers(TickResponse response) {
        String goodName = getTickResponseGoodName(response);
        if (validateStringData(goodName)) {
            Set<StreamObserver<TickResponse>> subscribers = subscriptions.get(goodName);
            sendNotification(response, subscribers);
        } else {
            LOGGER.warn("Illegal value {} for good by response: {}.", goodName, response);
            throw new NotFoundException("Illegal good value!");
        }
    }

    private boolean validateStringData(String goodName) {
        return goodName != null && !goodName.trim().isEmpty();
    }

    @Override
    public String getRequestGoodName(MarketDataRequest request) {
        return request.getGoodName().split("/")[1];
    }

    @Override
    public String getTickResponseGoodName(TickResponse response) {
        return response.getGoodName();
    }

    public HashSet<StreamObserver<TickResponse>> getSubscribers(String itemName) {
        return subscriptions.get(itemName);
    }

    private void addSubscriber(StreamObserver<TickResponse> responseObserver, String goodName) {
        subscriptions.compute(goodName, (key, value) -> {
            value = (value == null ? new HashSet<>() : value);
            value.add(responseObserver);
            return value;
        });
    }

    private void removeSubscriber(StreamObserver<TickResponse> responseObserver, String goodName) {
        this.subscriptions.compute(goodName, (key, value) -> {
            if (this.subscriptions.get(goodName) == null) {
                throw new NotFoundException("No such good!");
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
    }

    private void sendNotification(TickResponse response, Set<StreamObserver<TickResponse>> subscribers) {
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
    }
}
