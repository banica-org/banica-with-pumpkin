package com.market.banica.generator.service.grpc;

import com.market.AvailabilityResponse;
import com.market.BuySellProductResponse;
import com.market.CatalogueRequest;
import com.market.CatalogueResponse;
import com.market.MarketDataRequest;
import com.market.MarketServiceGrpc;
import com.market.ProductBuyRequest;
import com.market.ProductSellRequest;
import com.market.TickResponse;
import com.market.banica.generator.model.MarketTick;
import com.market.banica.generator.service.MarketState;
import com.market.banica.generator.service.SubscriptionManager;
import io.grpc.Status;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class MarketService extends MarketServiceGrpc.MarketServiceImplBase {

    private final SubscriptionManager subscriptionManager;

    private final MarketState marketState;

    private final Map<String, Map<Double, MarketTick>> waitingOrders = new HashMap<>();

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    @Autowired
    public MarketService(SubscriptionManager subscriptionManager, MarketState marketState) {
        this.subscriptionManager = subscriptionManager;
        this.marketState = marketState;
    }

    @Override
    public void subscribeForItem(MarketDataRequest request, StreamObserver<TickResponse> responseObserver) {
//        boolean hasSuccessfulBootstrap = bootstrapGeneratedTicks(request, responseObserver);
//        if (hasSuccessfulBootstrap) {
        subscriptionManager.subscribe(request, responseObserver);
//        }
    }

    @Override
    public void buyProduct(ProductBuyRequest request, StreamObserver<BuySellProductResponse> responseObserver) {
        try {
            lock.writeLock().lock();
            Map<Double, MarketTick> productInfo = waitingOrders.get(request.getItemName());
            if (productInfo.size() == 1) {
                waitingOrders.remove(request.getItemName());
            } else {
                long quantity = productInfo.get(request.getItemPrice()).getQuantity();
                if (quantity == request.getItemQuantity()) {
                    productInfo.remove(request.getItemPrice());
                } else {
                    productInfo.put(request.getItemPrice(),
                            new MarketTick(request.getItemName(), quantity - request.getItemQuantity(), request.getItemPrice(), request.getTimestamp()));
                }
            }

            responseObserver.onNext(BuySellProductResponse
                    .newBuilder()
                    .setMessage("Product " + request.getItemName() + " bought")
                    .build());
            responseObserver.onCompleted();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void sellProduct(ProductSellRequest request, StreamObserver<BuySellProductResponse> responseObserver) {
        marketState.addGoodToState(request.getItemName(), request.getItemPrice(), request.getItemQuantity(), request.getTimestamp());
        responseObserver
                .onNext(BuySellProductResponse
                        .newBuilder()
                        .setMessage("Sold " + request.getItemName() + " successfully.")
                        .build());
        responseObserver.onCompleted();
    }

    @Override
    public void checkAvailability(ProductBuyRequest request, StreamObserver<AvailabilityResponse> responseObserver) {
        boolean isAvailable = false;
        MarketTick marketTick = new MarketTick();
        try {
            marketTick = marketState.removeItemFromState(request.getItemName(), request.getItemQuantity(), request.getItemPrice());
            addItemToPending(request, marketTick.getTimestamp());
            isAvailable = true;
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

        responseObserver.onNext(AvailabilityResponse.newBuilder().setIsAvailable(isAvailable)
                .setItemName(request.getItemName())
                .setItemPrice(request.getItemPrice())
                .setItemQuantity(request.getItemQuantity())
                .setMarketName(request.getOrigin().toString())
                .setTimestamp(marketTick.getTimestamp())
                .build());
        responseObserver.onCompleted();
    }

    private void addItemToPending(ProductBuyRequest request, long timestamp) {
        Map<Double, MarketTick> pendingProductInfo = waitingOrders.get(request.getItemName());
        MarketTick tick;
        if (pendingProductInfo == null) {
            pendingProductInfo = new TreeMap<>();
            tick = new MarketTick(request.getItemName(), request.getItemQuantity(), request.getItemPrice(), timestamp);
            pendingProductInfo.put(request.getItemPrice(), tick);
            waitingOrders.put(request.getItemName(), pendingProductInfo);
        } else {
            MarketTick currentMarketTick = pendingProductInfo.get(request.getItemPrice());
            tick = new MarketTick(request.getItemName(), currentMarketTick.getQuantity() + request.getItemQuantity(), request.getItemPrice(), timestamp);
            pendingProductInfo.put(request.getItemPrice(), tick);
        }
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
