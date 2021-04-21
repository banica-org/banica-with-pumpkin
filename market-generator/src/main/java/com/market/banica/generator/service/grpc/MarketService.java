package com.market.banica.generator.service.grpc;

import com.market.BuySellProductResponse;
import com.market.CatalogueRequest;
import com.market.CatalogueResponse;
import com.market.MarketDataRequest;
import com.market.MarketServiceGrpc;
import com.market.ProductBuyRequest;
import com.market.TickResponse;
import com.market.banica.generator.service.MarketState;
import com.market.banica.generator.service.SubscriptionManager;
import io.grpc.Status;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MarketService extends MarketServiceGrpc.MarketServiceImplBase {

    private final SubscriptionManager subscriptionManager;

    private final MarketState marketState;

    @Autowired
    public MarketService(SubscriptionManager subscriptionManager, MarketState marketState) {
        this.subscriptionManager = subscriptionManager;
        this.marketState = marketState;
    }

    @Override
    public void subscribeForItem(MarketDataRequest request, StreamObserver<TickResponse> responseObserver) {
        boolean hasSuccessfulBootstrap = bootstrapGeneratedTicks(request, responseObserver);
        if (hasSuccessfulBootstrap) {
            subscriptionManager.subscribe(request, responseObserver);
        }
    }

    @Override
    public void buyProduct(ProductBuyRequest request, StreamObserver<BuySellProductResponse> responseObserver) {
        marketState.publishUpdate(request.getItemName(), -request.getItemQuantity(), request.getItemPrice());
        responseObserver.onNext(BuySellProductResponse.newBuilder().setMessage("Bought successfully!").build());
        responseObserver.onCompleted();
    }

    @Override
    public void requestCatalogue(CatalogueRequest request, StreamObserver<CatalogueResponse> responseObserver) {
        super.requestCatalogue(request, responseObserver);
    }

    private boolean bootstrapGeneratedTicks(MarketDataRequest request,
                                            StreamObserver<TickResponse> responseStreamObserver) {
        String goodName = request.getGoodName();
        ServerCallStreamObserver<TickResponse> cancellableSubscriber = (ServerCallStreamObserver<TickResponse>) responseStreamObserver;
        for (TickResponse tick : marketState.generateMarketTicks(goodName)) {
            if (cancellableSubscriber.isCancelled()) {
                responseStreamObserver.onError(Status.CANCELLED
                        .withDescription(responseStreamObserver + " has stopped requesting product " + goodName)
                        .asException());
                return false;
            }
            responseStreamObserver.onNext(tick);
        }
        return true;
    }

}
