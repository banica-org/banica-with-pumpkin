package com.market.banica.generator.service;

import com.market.CatalogueRequest;
import com.market.CatalogueResponse;
import com.market.MarketDataRequest;
import com.market.MarketServiceGrpc;
import com.market.TickResponse;
import com.market.banica.generator.tick.TickGenerator;
import com.market.banica.generator.tick.TickGeneratorImpl;
import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class MarketService extends MarketServiceGrpc.MarketServiceImplBase {

    private final MarketSubscriptionManager marketSubscriptionManager;

    private TickGeneratorImpl tickGenerator;

    @Autowired
    public MarketService(MarketSubscriptionManager marketSubscriptionManager, TickGeneratorImpl tickGenerator) {
        this.marketSubscriptionManager = marketSubscriptionManager;
        this.tickGenerator = tickGenerator;
    }

    @Override
    public void subscribeForItem(MarketDataRequest request, StreamObserver<TickResponse> responseObserver) {
//        String goodName = marketSubscriptionManager.getRequestGoodName(request);
        //europe.eggs
        tickGenerator.generateTicks(request.getGoodName()).forEach(responseObserver::onNext);
        marketSubscriptionManager.subscribe(request, responseObserver);
        responseObserver.onCompleted();
    }

    @Override
    public void requestCatalogue(CatalogueRequest request, StreamObserver<CatalogueResponse> responseObserver) {
        int[] index = new int[1];
        List<String> marketCatalogue = tickGenerator.getMarketCatalogue(request.getClientId());
        CatalogueResponse build = CatalogueResponse.newBuilder().addAllFoodItems(marketCatalogue).build();
        responseObserver.onNext(build);
        responseObserver.onCompleted();
    }
}
