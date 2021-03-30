package com.market.banica.generator.service;
import com.market.CatalogueRequest;
import com.market.CatalogueResponse;
import com.market.MarketDataRequest;
import com.market.MarketServiceGrpc;
import com.market.TickResponse;
import com.market.banica.generator.service.tickgeneration.TickGenerator;
import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MarketServiceImpl extends MarketServiceGrpc.MarketServiceImplBase implements MarketService {

    private final MarketSubscriptionManager marketSubscriptionManager;

    private final TickGenerator tickGenerator;

    private final MarketState marketState;

    @Autowired
    public MarketServiceImpl(MarketSubscriptionManager marketSubscriptionManager, TickGenerator tickGenerator, MarketState marketState) {
        this.marketSubscriptionManager = marketSubscriptionManager;
        this.tickGenerator = tickGenerator;
        this.marketState = marketState;
    }

    @Override
    public void subscribeForItem(MarketDataRequest request, StreamObserver<TickResponse> responseObserver) {
        String topic = marketSubscriptionManager.getRequestGoodName(request);
        marketState.generateMarketTicks(topic).forEach(responseObserver::onNext);
        marketSubscriptionManager.subscribe(request, responseObserver);
    }

    @Override
    public void requestCatalogue(CatalogueRequest request, StreamObserver<CatalogueResponse> responseObserver) {
        super.requestCatalogue(request, responseObserver);
    }
}
