package com.market.banica.generator.service.grpc;

import com.market.AvailabilityResponse;
import com.market.BuySellProductResponse;
import com.market.CatalogueRequest;
import com.market.CatalogueResponse;
import com.market.MarketDataRequest;
import com.market.MarketServiceGrpc;
import com.market.ProductBuySellRequest;
import com.market.TickResponse;
import com.market.banica.common.exception.ProductNotAvailableException;
import com.market.banica.generator.model.MarketTick;
import com.market.banica.generator.service.MarketState;
import com.market.banica.generator.service.SubscriptionManager;
import io.grpc.Status;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
@ManagedResource
public class MarketService extends MarketServiceGrpc.MarketServiceImplBase {

    private final SubscriptionManager subscriptionManager;

    private final MarketState marketState;

    private final Map<String, Map<Double, MarketTick>> pendingOrders = new HashMap<>();

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

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
    public void buyProduct(ProductBuySellRequest request, StreamObserver<BuySellProductResponse> responseObserver) {

        cleanPendingOrdersCollection(request);

        BuySellProductResponse buySellProductResponse = BuySellProductResponse.newBuilder().setMessage(String.format("Item with name %s was successfully bought from %s market.", request.getItemName(), request.getMarketName())).build();

        responseObserver.onNext(buySellProductResponse);
        responseObserver.onCompleted();

    }

    @Override
    public void returnPendingProduct(ProductBuySellRequest request, StreamObserver<BuySellProductResponse> responseObserver) {

        cleanPendingOrdersCollection(request);
        marketState.addGoodToState(request.getItemName(), request.getItemPrice(), request.getItemQuantity(), request.getTimestamp());

        BuySellProductResponse buySellProductResponse = BuySellProductResponse.newBuilder().setMessage(String.format("Item with name %s was successfully returned to market.", request.getItemName())).build();

        responseObserver.onNext(buySellProductResponse);
        responseObserver.onCompleted();
    }

    private void cleanPendingOrdersCollection(ProductBuySellRequest request) {
        try {
            lock.writeLock().lock();
            String productName = request.getItemName();
            double productPrice = request.getItemPrice();
            long productQuantity = request.getItemQuantity();

            MarketTick marketTick = pendingOrders.get(productName).get(productPrice);

            if (marketTick.getQuantity() > productQuantity) {
                long newMarketTickQuantity = marketTick.getQuantity() - productQuantity;

                MarketTick newMarketTick = new MarketTick(marketTick.getGood(), newMarketTickQuantity, marketTick.getPrice(), marketTick.getTimestamp());

                pendingOrders.get(productName).put(productPrice, newMarketTick);
            } else {
                pendingOrders.get(productName).remove(productPrice);
            }

            if (pendingOrders.get(productName).isEmpty()) {
                pendingOrders.remove(productName);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void checkAvailability(ProductBuySellRequest request, StreamObserver<AvailabilityResponse> responseObserver) {
        boolean isAvailable = false;
        MarketTick marketTick = new MarketTick();

        String productName = request.getItemName();
        double productPrice = request.getItemPrice();
        long productQuantity = request.getItemQuantity();
        String marketName = request.getMarketName();

        try {
            marketTick = marketState.removeItemFromState(productName, productQuantity, productPrice);
            addItemToPending(request, marketTick.getTimestamp());
            isAvailable = true;
        } catch (ProductNotAvailableException e) {
            e.printStackTrace();
        }

        AvailabilityResponse availabilityResponse = AvailabilityResponse.newBuilder()
                .setIsAvailable(isAvailable)
                .setItemName(productName)
                .setItemPrice(productPrice)
                .setItemQuantity(productQuantity)
                .setMarketName(marketName)
                .setTimestamp(marketTick.getTimestamp())
                .build();

        responseObserver.onNext(availabilityResponse);
        responseObserver.onCompleted();
    }

    private void addItemToPending(ProductBuySellRequest request, long timestamp) {
        Map<Double, MarketTick> pendingProductInfo = pendingOrders.get(request.getItemName());
        MarketTick tick;

        if (pendingProductInfo == null) {
            pendingProductInfo = new TreeMap<>();
            pendingOrders.put(request.getItemName(), pendingProductInfo);
        }
        if (!pendingProductInfo.containsKey(request.getItemPrice())) {
            pendingProductInfo.put(request.getItemPrice(), new MarketTick());
        }

        tick = pendingProductInfo.get(request.getItemPrice());
        MarketTick newMarketTick = new MarketTick(request.getItemName(), tick.getQuantity() + request.getItemQuantity(), request.getItemPrice(), timestamp);
        pendingProductInfo.put(request.getItemPrice(), newMarketTick);
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
