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

    /* public MarketService(MarketSubscriptionManager marketSubscriptionManager) {
        this.marketSubscriptionManager = marketSubscriptionManager;
        tickGenerator = new TickGenerator() {
            @Override
            public List<TickResponse> generateTicks(String topic) {
                List<TickResponse> list = new ArrayList<>();
                Random r = new Random();
                String name = topic.split("/")[1];
                for (int i = 0; i < 10; i++) {
                    list.add(TickResponse.newBuilder().setGoodName(name + (i + 1))
                            .setQuantity(r.nextInt(3))
                            .setPrice(r.nextDouble()).build());
                }
                return list;
            }
        };
    }*/

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
        super.requestCatalogue(request, responseObserver);
    }
}
