package com.market.banica.generator.service;

import com.aurora.Aurora;
import com.google.protobuf.Any;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(MarketSubscriptionManager.class);

    private final Map<String, Set<StreamObserver<Aurora.AuroraResponse>>> subscriptions = new ConcurrentHashMap<>();

    @Override
    public void subscribe(Aurora.AuroraRequest request, StreamObserver<Aurora.AuroraResponse> responseObserver) {
        String goodName = getGoodNameFromRequest(request);
        LOGGER.debug("{} Requested for subscription for good: {}.", responseObserver, goodName);
        addSubscriber(responseObserver, goodName);
    }

    @Override
    public void notifySubscribers(TickResponse response) {
        if (subscriptions.containsKey(response.getGoodName())) {
            sendNotification(response, subscriptions.get(response.getGoodName()));
        }
    }

    @Override
    public String getGoodNameFromRequest(Aurora.AuroraRequest request) {
        String[] topic = request.getTopic().split("/");
        if (topic.length == 2 && topic[1] != null && topic[1].length() > 0) {
            return topic[1];
        }

        LOGGER.warn("Illegal topic {} for good by request: {}.", request.getTopic(), request);
        throw new IllegalArgumentException("Illegal good value!");
    }

    @Override
    public Aurora.AuroraResponse convertTickResponseToAuroraResponse(TickResponse tickResponse) {
        return Aurora.AuroraResponse.newBuilder()
                .setMessage(Any.pack(tickResponse))
                .build();
    }

    private void addSubscriber(StreamObserver<Aurora.AuroraResponse> responseObserver, String goodName) {
        subscriptions.putIfAbsent(goodName, ConcurrentHashMap.newKeySet());
        subscriptions.get(goodName).add(responseObserver);
    }

    private void sendNotification(TickResponse response, Set<StreamObserver<Aurora.AuroraResponse>> subscribers) {
        for (StreamObserver<Aurora.AuroraResponse> currentSubscriber : subscribers) {
            ServerCallStreamObserver<Aurora.AuroraResponse> cancellableSubscriber = (ServerCallStreamObserver<Aurora.AuroraResponse>) currentSubscriber;
            if (cancellableSubscriber.isCancelled()) {
                currentSubscriber.onError(Status.CANCELLED
                        .withDescription(currentSubscriber + " has stopped requesting product " + response.getGoodName())
                        .asException());
                subscribers.remove(currentSubscriber);
                LOGGER.debug("Subscriber {} unsubscribed.", currentSubscriber);
                continue;
            }

            try {
                currentSubscriber.onNext(convertTickResponseToAuroraResponse(response));
            } catch (StatusRuntimeException e) {
                subscribers.remove(currentSubscriber);
                LOGGER.debug("Subscriber {} unsubscribed.", currentSubscriber);
            }
        }
        LOGGER.debug("Notified subscribers successfully with: {}.", response);
    }

}
