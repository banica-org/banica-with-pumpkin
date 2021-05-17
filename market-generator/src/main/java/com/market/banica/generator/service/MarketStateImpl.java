package com.market.banica.generator.service;

import com.market.TickResponse;
import com.market.banica.common.exception.ProductNotAvailableException;
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
        this.marketSnapshot = snapshotPersistence.loadMarketSnapshot();
        this.marketState = snapshotPersistence.loadMarketState(marketSnapshot);
        this.executorService = Executors.newSingleThreadExecutor();
        this.subscriptionManager = subscriptionManager;
        persistScheduler = new PersistScheduler(marketDataLock, snapshotPersistence, marketState, marketSnapshot);
        persistScheduler.scheduleSnapshot();
    }


    @Override
    public void addTickToMarket(MarketTick marketTick) {
        executorService.execute(() -> {
            try {
                marketDataLock.writeLock().lock();
                marketSnapshot.add(marketTick);

                String good = marketTick.getGood();
                marketState.putIfAbsent(good, new TreeSet<>());
                marketState.get(good).add(marketTick);

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

            LOGGER.info("Generating market ticks for {} .", good);
            return marketState.getOrDefault(good, new TreeSet<>()).stream()
                    .map(this::convertMarketTickToTickResponse)
                    .collect(Collectors.toList());

        } finally {
            marketDataLock.readLock().unlock();
        }
    }

    public PersistScheduler getPersistScheduler() {
        return persistScheduler;
    }

    @Override
    public MarketTick removeItemFromState(String itemName, long itemQuantity, double itemPrice) throws ProductNotAvailableException {
        MarketTick desireProduct;
        try {
            marketDataLock.writeLock().lock();

            Set<MarketTick> marketTicks = marketState.get(itemName);
            List<MarketTick> marketTicksWihEqualPrices = marketTicks
                    .stream()
                    .filter(marketTick -> marketTick.getPrice() == itemPrice)
                    .collect(Collectors.toList());
            long marketStateTicksQuantity = getQuantityForMarketTicksWithSamePrice(marketTicksWihEqualPrices);

            if (marketStateTicksQuantity < itemQuantity || marketTicksWihEqualPrices.size() == 0) {
                throw new ProductNotAvailableException(String.format("Product with name %s, price %.2f and quantity %d doesn't exist.",
                        itemName,
                        itemPrice,
                        itemQuantity));
            }

            MarketTick marketTick = new MarketTick(itemName, -itemQuantity, itemPrice, System.currentTimeMillis());
            this.addTickToMarket(marketTick);
            desireProduct = new MarketTick(itemName, itemQuantity, itemPrice, System.currentTimeMillis());
        } finally {
            marketDataLock.writeLock().unlock();
        }
        return desireProduct;
    }

    @Override
    public void addProductToMarketState(String itemName, double itemPrice, long itemQuantity) {
        MarketTick marketTick = new MarketTick(itemName, itemQuantity, itemPrice, System.currentTimeMillis());
        addTickToMarket(marketTick);
    }


    private long getQuantityForMarketTicksWithSamePrice(List<MarketTick> marketTicks) {
        return marketTicks.stream().mapToLong(MarketTick::getQuantity).sum();
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