package com.market.banica.generator.service;

import com.market.TickResponse;
import com.market.banica.generator.model.MarketTick;
import com.market.banica.generator.util.PersistScheduler;
import com.market.banica.generator.util.SnapshotPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

@Component
public class MarketStateImpl implements MarketState {

    private static final Logger LOGGER = LoggerFactory.getLogger(System.getenv("MARKET") + "." + MarketStateImpl.class.getSimpleName());

    private static final ReadWriteLock marketDataLock = new ReentrantReadWriteLock();

    private final SnapshotPersistence snapshotPersistence;

    private final Map<String, Set<MarketTick>> marketState;
    private final Queue<MarketTick> marketSnapshot;

    private final ExecutorService executorService;

    private final MarketSubscriptionManager subscriptionManager;

    private final PersistScheduler persistScheduler;

    @Autowired
    public MarketStateImpl(@Value("${tick.market.state.file.name}") String stateFileName,
                           @Value("${tick.market.snapshot.file.name}") String snapshotFileName,
                           MarketSubscriptionManager subscriptionManager) throws IOException {
        snapshotPersistence = new SnapshotPersistence(stateFileName, snapshotFileName);
        this.marketState = snapshotPersistence.loadMarketState();
        this.marketSnapshot = snapshotPersistence.loadMarketSnapshot();
        this.executorService = Executors.newSingleThreadExecutor();
        this.subscriptionManager = subscriptionManager;
        persistScheduler = new PersistScheduler(marketDataLock, snapshotPersistence, marketState, marketSnapshot);
        persistScheduler.scheduleSnapshot();
    }

    public void publishUpdate(String itemName, long itemQuantity, double itemPrice) {
        MarketTick marketTick = new MarketTick(itemName, itemQuantity, itemPrice, System.currentTimeMillis());
        subscriptionManager.notifySubscribers(convertMarketTickToTickResponse(marketTick));
    }


    @Override
    public void addTickToMarketSnapshot(MarketTick marketTick) {
        executorService.execute(() -> {
            try {
                marketDataLock.writeLock().lock();
                marketSnapshot.add(marketTick);

                snapshotPersistence.persistMarketSnapshot(marketSnapshot);

                subscriptionManager.notifySubscribers(convertMarketTickToTickResponse(marketTick));

            } catch (IOException e) {
                LOGGER.error("Could not persist market snapshot due to: {}", e.getMessage());
            } finally {
                marketDataLock.writeLock().unlock();
            }
        });
    }

    @Override
    public List<TickResponse> generateMarketTicks(String good) {
        try {
            marketDataLock.readLock().lock();

            List<TickResponse> generatedTicks = marketState.getOrDefault(good, new TreeSet<>()).stream()
                    .map(this::convertMarketTickToTickResponse)
                    .collect(Collectors.toList());

//            marketSnapshot.stream()
//                    .filter(marketTick -> marketTick.getGood().equals(good))
//                    .map(this::convertMarketTickToTickResponse)
//                    .forEach(generatedTicks::add);

            LOGGER.info("Successfully generated market ticks for {} .", good);
            return generatedTicks;

        } finally {
            marketDataLock.readLock().unlock();
        }
    }

    public PersistScheduler getPersistScheduler() {
        return persistScheduler;
    }

    @Override
    public MarketTick removeItemFromState(String itemName, long itemQuantity, double itemPrice) {
        try {
            marketDataLock.writeLock().lock();
            Set<MarketTick> productInfo = marketState.get(itemName);
            MarketTick desiredProduct = productInfo
                    .stream()
                    .filter(marketTick -> marketTick.getQuantity() == itemQuantity && marketTick.getPrice() == itemPrice)
                    .findFirst().orElse(null);

            long availableQuantity = 0;
            if (desiredProduct == null ||
                    desiredProduct.getQuantity() < itemQuantity) {
                throw new IllegalArgumentException("No product " + itemName + " with price " + itemPrice
                        + " and quantity " + itemQuantity + ".");
            }
            productInfo.remove(desiredProduct);
            desiredProduct = new MarketTick(itemName, availableQuantity - itemQuantity, itemPrice, desiredProduct.getTimestamp());
            productInfo.add(desiredProduct);
            if (desiredProduct.getPrice() <= 0) {
                productInfo.remove(desiredProduct);
            }
            if (productInfo.size() == 0) {
                marketState.remove(itemName);
            }

            publishUpdate(itemName, -itemQuantity, itemPrice);
            return desiredProduct;
        } finally {

            marketDataLock.writeLock().lock();
        }
    }

    @Override
    public void addGoodToState(String itemName, double itemPrice, long itemQuantity, long timestamp) {
        try {
            marketDataLock.writeLock().lock();
            Set<MarketTick> productInfo = marketState.get(itemName);
            if (productInfo == null) {
                productInfo = new TreeSet<>();
                productInfo.add(new MarketTick(itemName, itemQuantity, itemPrice, timestamp));
                marketState.put(itemName, productInfo);
            } else {
                MarketTick tick = new MarketTick(itemName, itemQuantity, itemPrice, timestamp);
                if (productInfo.contains(tick)) {
                    MarketTick searchedTick = productInfo.stream().filter(currentTick -> currentTick.compareTo(tick) == 0).findFirst().orElse(null);
                    productInfo.remove(searchedTick);
                    productInfo.add(new MarketTick(itemName, itemQuantity + searchedTick.getQuantity(), itemPrice, timestamp));
                } else {
                    productInfo.add(tick);
                }
            }
            publishUpdate(itemName, itemQuantity, itemPrice);
        } finally {
            marketDataLock.writeLock().unlock();
        }
    }

    private TickResponse convertMarketTickToTickResponse(MarketTick marketTick) {
        return TickResponse.newBuilder()
                .setOrigin(MarketTick.getOrigin())
                .setTimestamp(marketTick.getTimestamp())
                .setGoodName(marketTick.getGood())
                .setPrice(marketTick.getPrice())
                .setQuantity(marketTick.getQuantity())
                .build();
    }

    @PreDestroy
    private void onDestroy() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

}