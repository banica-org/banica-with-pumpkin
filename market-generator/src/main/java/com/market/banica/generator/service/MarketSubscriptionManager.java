package com.market.banica.generator.service;

import com.market.MarketDataRequest;
import com.market.TickResponse;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MarketSubscriptionManager implements SubscriptionManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(System.getenv("MARKET") + "." + MarketSubscriptionManager.class.getSimpleName());

    private final Map<String, Set<StreamObserver<TickResponse>>> subscriptions = new ConcurrentHashMap<>();

    @Override
    public void subscribe(MarketDataRequest request, StreamObserver<TickResponse> responseObserver) {
        String goodName = request.getGoodName();
        LOGGER.debug("{} Requested for subscription for good: {}.", responseObserver, goodName);
        addSubscriber(responseObserver, goodName);
    }

    @Override
    public void notifySubscribers(TickResponse response) {
        if (subscriptions.containsKey(response.getGoodName())) {
            sendNotification(response, subscriptions.get(response.getGoodName()));
        }
    }

    private void addSubscriber(StreamObserver<TickResponse> responseObserver, String goodName) {
        subscriptions.putIfAbsent(goodName, ConcurrentHashMap.newKeySet());
        subscriptions.get(goodName).add(responseObserver);
    }

    private void sendNotification(TickResponse response, Set<StreamObserver<TickResponse>> subscribers) {
        for (StreamObserver<TickResponse> currentSubscriber : subscribers) {
            ServerCallStreamObserver<TickResponse> cancellableSubscriber = (ServerCallStreamObserver<TickResponse>) currentSubscriber;
            if (cancellableSubscriber.isCancelled()) {
                currentSubscriber.onError(Status.CANCELLED
                        .withDescription(currentSubscriber + " has stopped requesting product " + response.getGoodName())
                        .asException());
                subscribers.remove(currentSubscriber);
                LOGGER.debug("Subscriber {} unsubscribed.", currentSubscriber);
                continue;
            }

            try {
                    currentSubscriber.onNext(response);
            } catch (StatusRuntimeException e) {
                subscribers.remove(currentSubscriber);
                LOGGER.debug("Subscriber {} unsubscribed.", currentSubscriber);
            }
        }
        LOGGER.debug("Notified subscribers successfully with: {}.", response);
    }

}
