package com.market.banica.order.book.model;

import com.aurora.Aurora;

import com.market.banica.common.channel.ChannelRPCConfig;
import com.market.banica.common.validator.DataValidator;
import com.orderbook.OrderBookLayer;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component
public class ItemMarket {

    private static final Logger LOGGER = LogManager.getLogger(ItemMarket.class);
    private static final int MAX_RETRY_ATTEMPTS = 1000;
    private final String orderBookGrpcPort;

    private final ManagedChannel managedChannel;

    private final Map<String, TreeSet<Item>> allItems;
    private final Map<String, Long> productsQuantity;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final AtomicInteger numberOfTicksToProcess = new AtomicInteger(0);

    private final AtomicBoolean backPressureStarted = new AtomicBoolean(false);

    private final ExecutorService itemProcessingExecutor;

    @Autowired
    public ItemMarket(@Value("${item.processing.executor.pool.size}") final int poolSize,
                      @Value("${aurora.server.host}") final String host,
                      @Value("${aurora.server.port}") final int port,
                      @Value("${orderbook.server.port}") final int orderBookGrpcPort) {

        this.allItems = new ConcurrentHashMap<>();
        this.productsQuantity = new ConcurrentHashMap<>();
        this.itemProcessingExecutor = Executors.newFixedThreadPool(poolSize);

        managedChannel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .defaultServiceConfig(ChannelRPCConfig.getInstance().getServiceConfig())
                .enableRetry()
                .maxRetryAttempts(MAX_RETRY_ATTEMPTS)
                .build();

        this.orderBookGrpcPort = String.valueOf(orderBookGrpcPort);
    }

    public void updateItem(Aurora.AuroraResponse response) {
        LOGGER.debug("Processing AuroraResponse: {}", response);
        numberOfTicksToProcess.incrementAndGet();
        itemProcessingExecutor.execute(new ItemProcessingTask(response, allItems, productsQuantity,
                numberOfTicksToProcess, managedChannel, backPressureStarted, orderBookGrpcPort));
    }

    public List<OrderBookLayer> getRequestedItem(String itemName, long quantity) {

        LOGGER.info("Getting requested item: {} with quantity: {}", itemName, quantity);

        DataValidator.validateIncomingData(itemName);

        TreeSet<Item> items = this.allItems.get(itemName);

        if (items == null || this.productsQuantity.get(itemName) < quantity) {

            return Collections.emptyList();
        }

        List<OrderBookLayer> layers;
        try {
            lock.readLock().lock();

            layers = new ArrayList<>();

            Iterator<Item> iterator = items.iterator();
            long itemLeft = quantity;

            while (itemLeft > 0) {
                Item currentItem = iterator.next();

                OrderBookLayer.Builder currentLayer = populateItemLayer(itemLeft, currentItem);

                itemLeft -= currentLayer.getQuantity();

                OrderBookLayer orderBookLayer = currentLayer
                        .setOrigin(currentItem.getOrigin())
                        .build();
                layers.add(orderBookLayer);
            }
        } finally {
            lock.readLock().unlock();
        }
        return layers;
    }

    public Map<String, Long> getProductsQuantity() {
        return productsQuantity;
    }

    public Optional<Set<Item>> getItemSetByName(String itemName) {
        return Optional.ofNullable(this.allItems.get(itemName));
    }

    public Set<String> getItemNameSet() {
        return this.allItems.keySet();
    }

    public void addTrackedItem(String itemName) {
        this.allItems.put(itemName, new TreeSet<>());
        this.productsQuantity.put(itemName, 0L);
    }

    public void removeUntrackedItem(String itemName) {
        this.allItems.remove(itemName);
        this.productsQuantity.remove(itemName);
    }

    public void zeroingMarketProductsFromMarket(String marketDestination, String itemName) {
        try {
            lock.writeLock().lock();

            long removedItemProductQuantity = 0;
            final Iterator<Item> itemsIterator = allItems.get(itemName).iterator();

            while (itemsIterator.hasNext()) {
                Item currentItem = itemsIterator.next();
                if (currentItem.getOrigin().toString().equalsIgnoreCase(marketDestination.split("-")[1])) {
                    removedItemProductQuantity += currentItem.getQuantity();
                    itemsIterator.remove();
                }
            }

            productsQuantity.put(itemName, productsQuantity.get(itemName) - removedItemProductQuantity);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private OrderBookLayer.Builder populateItemLayer(long itemLeft, Item currentItem) {
        OrderBookLayer.Builder currentLayer = OrderBookLayer.newBuilder()
                .setPrice(currentItem.getPrice());

        currentLayer.setQuantity(Math.min(currentItem.getQuantity(), itemLeft));
        return currentLayer;
    }
}
