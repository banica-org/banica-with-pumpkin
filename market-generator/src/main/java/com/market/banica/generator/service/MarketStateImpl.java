package com.market.banica.generator.service;

import com.market.TickResponse;
import com.market.banica.generator.model.MarketTick;
import com.market.banica.generator.util.SnapshotPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(MarketStateImpl.class);

    private static final ReadWriteLock marketStateLock = new ReentrantReadWriteLock();

    private final Map<String, Set<MarketTick>> marketState;

    private final ExecutorService executorService;

    private final MarketSubscriptionManager subscriptionManager;

    @Autowired
    public MarketStateImpl(@Value("${tick.database.file.name}") String fileName,
                           MarketSubscriptionManager subscriptionManager) throws IOException {
        SnapshotPersistence snapshotPersistence = new SnapshotPersistence(fileName);
        this.marketState = snapshotPersistence.loadPersistedSnapshot();
        this.executorService = Executors.newSingleThreadExecutor();
        this.subscriptionManager = subscriptionManager;
        PersistScheduler persistScheduler = new PersistSchedulerImpl(marketStateLock, snapshotPersistence, marketState);
        persistScheduler.scheduleSnapshot();
    }

    @Override
    public void addTickToMarketState(MarketTick marketTick) {
        executorService.execute(() -> {
            try {
                marketStateLock.writeLock().lock();
                String good = marketTick.getGood();
                if (!marketState.containsKey(good)) {
                    marketState.put(good, new TreeSet<>(Comparator.comparingLong(MarketTick::getTimestamp)));
                }
                marketState.get(good).add(marketTick);
                subscriptionManager.notifySubscribers(convertMarketTickToTickResponse(marketTick));
            } finally {
                marketStateLock.writeLock().unlock();
            }
        });
    }

    @Override
    public List<TickResponse> generateMarketTicks(String good) {
        try {
            marketStateLock.readLock().lock();
            LOGGER.info("Generate market ticks called for good {} .", good);

            if (!marketState.containsKey(good)) {
                LOGGER.warn("Cannot generate ticks, No such good in market.");
                return Collections.emptyList();
            }

            LOGGER.info("Successfully generated market ticks for {} .", good);

            return marketState.get(good).stream()
                    .map(this::convertMarketTickToTickResponse)
                    .collect(Collectors.toList());
        } finally {
            marketStateLock.readLock().unlock();
        }
    }

    @Override
    public Map<String, Set<MarketTick>> getMarketState() {
        try {
            marketStateLock.readLock().lock();
            Map<String, Set<MarketTick>> currentMarketState = new HashMap<>();
            marketState.forEach((good, ticks) -> currentMarketState.put(good, new TreeSet<>(ticks)));
            return currentMarketState;
        } finally {
            marketStateLock.readLock().unlock();
        }
    }

    private TickResponse convertMarketTickToTickResponse(MarketTick marketTick) {
        return TickResponse.newBuilder()
                .setOrigin(MarketTick.getOrigin())
                .setTimestamp(System.currentTimeMillis())
                .setGoodName(marketTick.getGood())
                .setPrice(marketTick.getPrice())
                .setQuantity(marketTick.getQuantity())
                .build();
    }

    @PreDestroy
    private void onDestroy() {
        try {
            executorService.shutdown();
            executorService.awaitTermination(1, TimeUnit.SECONDS);
            executorService.shutdownNow();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}