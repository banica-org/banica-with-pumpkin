package com.market.banica.aurora.observer;

import com.market.TickResponse;
import com.market.banica.aurora.manager.MarketMapperManager;
import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MarketTickResponseObserver implements StreamObserver<TickResponse> {

    public final MarketMapperManager marketMapperManager;

    public String topic;

    @Autowired
    public MarketTickResponseObserver(MarketMapperManager marketMapperManager) {
        this.marketMapperManager = marketMapperManager;
    }

    @Override
    public void onNext(TickResponse tickResponse) {
        this.topic = tickResponse.getGoodName();
        marketMapperManager.notifyObservers(tickResponse, this);
    }

    @Override
    public void onError(Throwable throwable) {
        marketMapperManager.unsubscribe(this);
    }

    @Override
    public void onCompleted() {
        marketMapperManager.unsubscribe(this);
    }
}
