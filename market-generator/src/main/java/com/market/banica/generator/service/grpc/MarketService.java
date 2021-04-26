package com.market.banica.generator.service.grpc;

import com.market.AvailabilityResponse;
import com.market.BuySellProductResponse;
import com.market.CatalogueRequest;
import com.market.CatalogueResponse;
import com.market.MarketDataRequest;
import com.market.MarketServiceGrpc;
import com.market.ProductBuySellRequest;
import com.market.TickResponse;
import com.market.banica.generator.model.MarketTick;
import com.market.banica.generator.service.MarketState;
import com.market.banica.generator.service.SubscriptionManager;
import io.grpc.Status;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
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
        //  addDummyData();
    }

    @ManagedOperation
    public void printState() {
        this.marketState.getMarketState().entrySet().forEach(System.out::println);
    }

    private void addDummyData() {
        Map<String, Set<MarketTick>> marketState = this.marketState.getMarketState();
        long millis = System.currentTimeMillis();
        TreeSet<MarketTick> eggTicks = new TreeSet<>();
        eggTicks.add(new MarketTick("eggs", 20, 1.0, millis));
        eggTicks.add(new MarketTick("eggs", 20, 2.0, millis));
//        eggTicks.add(new MarketTick("eggs", 11, 1.0, millis));
        marketState.put("eggs", eggTicks);


        TreeSet<MarketTick> waterTicks = new TreeSet<>();
        waterTicks.add(new MarketTick("waterTicks", 5, 5.0, millis));
        marketState.put("water", waterTicks);

        TreeSet<MarketTick> crustTicks = new TreeSet<>();
        crustTicks.add(new MarketTick("crusts", 20, 2.0, millis));
        crustTicks.add(new MarketTick("crusts", 20, 1.0, millis));
//        crustTicks.add(new MarketTick("crusts", 1, 1.0, millis));
//        crustTicks.add(new MarketTick("crusts", 3, 1.0, millis + 1));
//        crustTicks.add(new MarketTick("crusts", 7, 1.0, millis + 2));
        marketState.put("crusts", crustTicks);

        TreeSet<MarketTick> banicaTicks = new TreeSet<>();
        banicaTicks.add(new MarketTick("banica", 2, 1.0, millis));
        banicaTicks.add(new MarketTick("banica", 2, 1.0, millis + 1));
        banicaTicks.add(new MarketTick("banica", 3, 1.0, millis + 2));
        marketState.put("banica", banicaTicks);
//        marketState.addGoodToState("eggs", 5.0, 400, millis);
//        this.marketState.addGoodToState("waterTicks", 5.0, 400, millis);
//        eggTicks.add(new MarketTick("eggs", 20, 5.0, millis));
        this.marketState.addGoodToState("tomatoes", 5.0, 70, millis);
        this.marketState.addGoodToState("milk", 5.0, 3, millis);
        this.marketState.addGoodToState("pumpkin", 5.0, 400, millis);
        this.marketState.addGoodToState("sugar", 5.0, 60, millis);
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

        responseObserver.onNext(BuySellProductResponse
                .newBuilder()
                .setMessage(String.format("Item with name %s was successfully bought from %s market.", request.getItemName(), request.getMarketName()))
                .build());
        responseObserver.onCompleted();

    }

    @Override
    public void returnPendingProduct(ProductBuySellRequest request, StreamObserver<BuySellProductResponse> responseObserver) {

        cleanPendingOrdersCollection(request);

        marketState.addGoodToState(request.getItemName(), request.getItemPrice(), request.getItemQuantity(), request.getTimestamp());

        responseObserver.onNext(BuySellProductResponse
                .newBuilder()
                .setMessage(String.format("Item with name %s was successfully returned to market", request.getItemName()))
                .build());
        responseObserver.onCompleted();
    }

    private void cleanPendingOrdersCollection(ProductBuySellRequest request) {
        try {
            lock.writeLock().lock();
            MarketTick marketTick = pendingOrders.get(request.getItemName()).get(request.getItemPrice());

            if (marketTick.getQuantity() > request.getItemQuantity()) {
                long newMarketTickQuantity = marketTick.getQuantity() - request.getItemQuantity();
                MarketTick newMarketTick = new MarketTick(marketTick.getGood(), newMarketTickQuantity, marketTick.getPrice(), marketTick.getTimestamp());
                pendingOrders.get(request.getItemName()).put(request.getItemPrice(), newMarketTick);
            } else {
                pendingOrders.get(request.getItemName()).remove(request.getItemPrice());
            }

            if (pendingOrders.get(request.getItemName()).isEmpty()) {
                pendingOrders.remove(request.getItemName());
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void checkAvailability(ProductBuySellRequest request, StreamObserver<AvailabilityResponse> responseObserver) {
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
                .setMarketName(request.getMarketName())
                .setTimestamp(marketTick.getTimestamp())
                .build());
        responseObserver.onCompleted();
    }

    private void addItemToPending(ProductBuySellRequest request, long timestamp) {
        Map<Double, MarketTick> pendingProductInfo = pendingOrders.get(request.getItemName());
        MarketTick tick;
        if (pendingProductInfo == null) {
            pendingProductInfo = new TreeMap<>();
            tick = new MarketTick(request.getItemName(), request.getItemQuantity(), request.getItemPrice(), timestamp);
            pendingProductInfo.put(request.getItemPrice(), tick);
            pendingOrders.put(request.getItemName(), pendingProductInfo);
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
