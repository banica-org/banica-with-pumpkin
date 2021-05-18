package com.market.banica.order.book.model;

import com.aurora.Aurora;
import com.aurora.AuroraServiceGrpc;
import com.google.protobuf.InvalidProtocolBufferException;
import com.market.TickResponse;
import com.market.banica.common.exception.IncorrectResponseException;
import com.market.banica.common.validator.DataValidator;
import com.market.banica.order.book.observer.BackPressureObserver;
import io.grpc.ManagedChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ItemProcessingTask implements Runnable {

    private static final Logger LOGGER = LogManager.getLogger(ItemProcessingTask.class);

    private static final String START_BACKPRESSURE_TOPIC = "market/on";
    private static final String STOP_BACKPRESSURE_TOPIC = "market/off";
    private static final int BACKPRESSURE_ACTIVATION_THRESHOLD = 1000;
    private static final int BACKPRESSURE_DEACTIVATION_THRESHOLD = 500;

    private final String orderBookGrpcPort;

    private final ManagedChannel managedChannel;

    private final Map<String, TreeSet<Item>> allItems;
    private final Map<String, Long> productsQuantity;

    private final Aurora.AuroraResponse response;

    private final ReentrantReadWriteLock lock;

    private final AtomicInteger numberOfTicksToProcess;
    private final AtomicBoolean backPressureStarted;

    public ItemProcessingTask(Aurora.AuroraResponse response, Map<String, TreeSet<Item>> allItems, Map<String, Long> productsQuantity,
                              AtomicInteger numberOfTicksToProcess, ManagedChannel managedChannel, AtomicBoolean backPressureStarted,
                              String orderBookGrpcPort, ReentrantReadWriteLock lock) {
        this.response = response;
        this.allItems = allItems;
        this.productsQuantity = productsQuantity;
        this.numberOfTicksToProcess = numberOfTicksToProcess;
        this.managedChannel = managedChannel;
        this.backPressureStarted = backPressureStarted;
        this.orderBookGrpcPort = orderBookGrpcPort;
        this.lock = lock;
    }

    @Override
    public void run() {
        try {
            lock.writeLock().lock();

            TickResponse tickResponse;

            try {
                tickResponse = response.getMessage().unpack(TickResponse.class);
            } catch (InvalidProtocolBufferException e) {
                throw new IncorrectResponseException("Incorrect response! Response must be from TickResponse type.");
            }

            String goodName = tickResponse.getGoodName();
            DataValidator.validateIncomingData(goodName);

            Set<Item> itemSet = this.allItems.get(goodName);
            if (itemSet == null) {
                LOGGER.error("Item: {} is not being tracked and cannot be added to itemMarket!",
                        goodName);
                return;
            }

            checkForBackpressureNeed();

            Item item = populateItem(tickResponse);

            this.productsQuantity.merge(goodName, tickResponse.getQuantity(), Long::sum);

            numberOfTicksToProcess.decrementAndGet();

            if (itemSet.contains(item)) {

                Item presentItem = itemSet.stream().filter(currentItem -> currentItem.compareTo(item) == 0).findFirst().get();
                presentItem.setQuantity(presentItem.getQuantity() + item.getQuantity());
                return;
            }
            itemSet.add(item);

        } finally {
            lock.writeLock().unlock();
        }
    }

    private void checkForBackpressureNeed() {
        synchronized (backPressureStarted) {
            if (numberOfTicksToProcess.get() >= BACKPRESSURE_ACTIVATION_THRESHOLD && !backPressureStarted.get()) {
                startBackPressure();
                backPressureStarted.set(true);
            } else if (numberOfTicksToProcess.get() <= BACKPRESSURE_DEACTIVATION_THRESHOLD && backPressureStarted.get()) {
                stopBackPressure();
                backPressureStarted.set(false);
            }
        }
    }

    private void startBackPressure() {
        LOGGER.info("ACTIVATING BACK PRESSURE!");
        final AuroraServiceGrpc.AuroraServiceStub asynchronousStub = getAsynchronousStub();

        Aurora.AuroraRequest request = Aurora.AuroraRequest.newBuilder()
                .setTopic(START_BACKPRESSURE_TOPIC)
                .setClientId(orderBookGrpcPort)
                .build();

        asynchronousStub.backpressure(request, new BackPressureObserver());
    }

    private void stopBackPressure() {
        LOGGER.info("DEACTIVATING BACK PRESSURE!");
        final AuroraServiceGrpc.AuroraServiceStub asynchronousStub = getAsynchronousStub();

        Aurora.AuroraRequest request = Aurora.AuroraRequest.newBuilder()
                .setTopic(STOP_BACKPRESSURE_TOPIC)
                .setClientId(orderBookGrpcPort)
                .build();

        asynchronousStub.backpressure(request, new BackPressureObserver());
    }

    private Item populateItem(TickResponse tickResponse) {

        Item item = new Item();
        item.setPrice(tickResponse.getPrice());
        item.setQuantity(tickResponse.getQuantity());
        item.setOrigin(tickResponse.getOrigin());

        return item;
    }

    private AuroraServiceGrpc.AuroraServiceStub getAsynchronousStub() {
        return AuroraServiceGrpc.newStub(managedChannel);
    }
}
