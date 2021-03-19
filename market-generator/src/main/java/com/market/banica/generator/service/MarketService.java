package com.market.banica.generator.service;

import com.market.CatalogueRequest;
import com.market.CatalogueResponse;
import com.market.MarketDataRequest;
import com.market.MarketServiceGrpc;
import com.market.TickResponse;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;

@Service
public class MarketService extends MarketServiceGrpc.MarketServiceImplBase {

    private MarketSubscriptionManager marketSubscriptionManager;

    private TickGenerator tickGenerator;

    @Override
    public void subscribeForItem(MarketDataRequest request, StreamObserver<TickResponse> responseObserver) {
        System.out.println("Yashaaaaaaaaaaa");
        String topic = marketSubscriptionManager.getRequestGoodName(request);
        tickGenerator.generateTicks(topic).forEach(responseObserver::onNext);
        marketSubscriptionManager.subscribe(request, responseObserver);
        responseObserver.onCompleted();
    }

    @Override
    public void requestCatalogue(CatalogueRequest request, StreamObserver<CatalogueResponse> responseObserver) {
        super.requestCatalogue(request, responseObserver);
    }
}
