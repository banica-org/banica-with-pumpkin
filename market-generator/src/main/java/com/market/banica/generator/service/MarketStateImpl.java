package com.market.banica.generator.service;

import com.market.Origin;
import com.market.TickResponse;

import com.market.banica.generator.model.MarketTick;
import com.market.banica.generator.service.snapshot.MarketSnapshot;
import com.market.banica.generator.service.tickgeneration.TickGenerator;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;


@Component
public class MarketStateImpl implements com.market.banica.generator.service.MarketState {

    private static final Logger LOGGER = LoggerFactory.getLogger(MarketStateImpl.class);

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    @Getter
    private final BlockingQueue<MarketTick> blockingQueue = new LinkedBlockingQueue<>();

    private final MarketSubscriptionManager subscriptionManager;

    private final MarketSnapshot marketSnapshot;

   // private final Set<MarketTick> marketState = new TreeSet<>(MarketTick::);

    private final ExecutorService executorService;

    private static final int THREAD_POOL_SIZE = 1;


    @Value("${origin}")
    private String origin;

    @Autowired
    public MarketStateImpl(TickGenerator tickGenerator, MarketSubscriptionManager subscriptionManager, MarketSnapshot marketSnapshot) {
        this.tickGenerator = tickGenerator;
        this.subscriptionManager = subscriptionManager;
        this.marketSnapshot = marketSnapshot;
        this.executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    }

    @PostConstruct
    private void loadLastMarketState() {
        LOGGER.info("Loading market state from snapshot...");
        this.marketState.putAll(marketSnapshot.getSnapshot());
        while (!executorService.isShutdown()) {
           // executorService.execute(); blocking,take()
        }
    }

    @Override
    public void addGoodToMarketState(String goodName, double goodPrice, long goodQuantity) {
        try {
            lock.writeLock().lock();
            LOGGER.info("Adding {} to market state.", goodName);
            if (marketState.containsKey(goodName)) {
                Map<Double, Long> goodInfo = marketState.get(goodName);
                if (goodInfo.containsKey(goodPrice)) {
                    Long currentGoodQuantityPerPriceInMap = goodInfo.get(goodPrice);
                    goodInfo.put(goodPrice, currentGoodQuantityPerPriceInMap + goodQuantity);
                } else {
                    goodInfo.put(goodPrice, goodQuantity);
                }
            } else {
                marketState.put(goodName, new TreeMap<>(Double::compareTo));
                marketState.get(goodName).put(goodPrice, goodQuantity);
            }
            updateSubscribers(goodName, goodQuantity, goodPrice);
            LOGGER.info("{} added to market state.", goodName);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public List<TickResponse> generateMarketTicks(String good) {
        try {
            lock.readLock().lock();
            LOGGER.info("Generate market ticks called for good {} .", good);

            if (this.marketState.get(good) == null) {
                LOGGER.debug("Cannot generate ticks, No such good in market.");
                return Collections.emptyList();
            }

            LOGGER.info("Successfully generated market ticks for {} .", good);

            return marketState.get(good).entrySet().stream()
                    .map(entry -> toResponse(good, entry.getKey(), entry.getValue()))
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    private TickResponse toResponse(String good, Double price, Long amount) {
        return TickResponse.newBuilder()
                .setOrigin(Origin.valueOf(origin.toUpperCase()))
                .setTimestamp(System.currentTimeMillis())
                .setGoodName(good)
                .setPrice(price)
                .setQuantity(amount)
                .build();
    }

    @Override
    public void updateSubscribers(String goodName, long goodQuantity, double goodPrice) {
        MarketTick marketTick = new MarketTick(origin, goodName, goodQuantity, goodPrice);
        subscriptionManager.notifySubscribers(marketTick.toResponse());
    }

    @Override
    public Map<String, Map<Double, Long>> getMarketState() {
        try {
            lock.readLock().lock();
            Map<String, Map<Double, Long>> currentMarketState = new HashMap<>();
            this.marketState.forEach((good, price) -> currentMarketState.put(good, new HashMap<>(price)));
            return currentMarketState;
        } finally {
            lock.readLock().unlock();
        }
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