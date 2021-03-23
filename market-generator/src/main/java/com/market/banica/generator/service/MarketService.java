package com.market.banica.generator.service;

import com.market.CatalogueRequest;
import com.market.CatalogueResponse;
import com.market.MarketDataRequest;
import com.market.MarketServiceGrpc;
import com.market.TickResponse;
import com.market.banica.generator.tick.TickGeneratorImpl;
import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MarketService extends MarketServiceGrpc.MarketServiceImplBase {

    private final MarketSubscriptionManager marketSubscriptionManager;

    private final TickGeneratorImpl tickGenerator;

    @Autowired
    public MarketService(MarketSubscriptionManager marketSubscriptionManager, TickGeneratorImpl tickGenerator) {
        this.marketSubscriptionManager = marketSubscriptionManager;
        this.tickGenerator = tickGenerator;
    }

    @Override
    public void subscribeForItem(MarketDataRequest request, StreamObserver<TickResponse> responseObserver) {
        tickGenerator.generateTicks(request.getGoodName()).forEach(responseObserver::onNext);
        marketSubscriptionManager.subscribe(request, responseObserver);
        responseObserver.onCompleted();
    }

    @Override
    public void requestCatalogue(CatalogueRequest request, StreamObserver<CatalogueResponse> responseObserver) {
        List<String> marketCatalogue = tickGenerator.getMarketCatalogue(request.getMarketOrigin());
        CatalogueResponse build = CatalogueResponse.newBuilder().addAllFoodItems(marketCatalogue).build();
        responseObserver.onNext(build);
        responseObserver.onCompleted();
    }
}
