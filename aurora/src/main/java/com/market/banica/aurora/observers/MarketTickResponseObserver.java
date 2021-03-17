package com.market.banica.aurora.observers;

import com.market.TickResponse;
import com.market.banica.aurora.service.AuroraSubscriptionManager;
import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MarketTickResponseObserver implements StreamObserver<TickResponse> {

    public final AuroraSubscriptionManager subscriptionManager;

    public String topic;

    @Autowired
    public MarketTickResponseObserver(AuroraSubscriptionManager subscriptionManager) {
        this.subscriptionManager = subscriptionManager;
    }

    @Override
    public void onNext(TickResponse tickResponse) {
        this.topic = tickResponse.getGoodName();
        subscriptionManager.notifyObservers(tickResponse, this);
    }

    @Override
    public void onError(Throwable throwable) {
        subscriptionManager.unsubscribe(this);
    }

    @Override
    public void onCompleted() {
        subscriptionManager.unsubscribe(this);
    }
}
