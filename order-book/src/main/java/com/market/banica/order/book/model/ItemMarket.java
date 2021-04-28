package com.market.banica.order.book.model;

import com.aurora.Aurora;
import com.google.protobuf.InvalidProtocolBufferException;
import com.market.TickResponse;

import com.market.banica.common.exception.IncorrectResponseException;
import com.market.banica.common.validator.DataValidator;
import com.orderbook.OrderBookLayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component
public class ItemMarket {

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<String, TreeSet<Item>> allItems;
    private final Map<String, Long> productsQuantity;

    private static final Logger LOGGER = LogManager.getLogger(ItemMarket.class);

    @Autowired
    public ItemMarket() {
        this.allItems = new ConcurrentHashMap<>();
        this.productsQuantity = new ConcurrentHashMap<>();
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

    public void updateItem(Aurora.AuroraResponse response) {

        if (!response.getMessage().is(TickResponse.class)) {
            throw new IncorrectResponseException("Response is not correct!");
        }

        TickResponse tickResponse;

        try {
            tickResponse = response.getMessage().unpack(TickResponse.class);
        } catch (InvalidProtocolBufferException e) {
            throw new IncorrectResponseException("Incorrect response! Response must be from IncorrectResponseException type.");
        }

        String goodName = tickResponse.getGoodName();
        DataValidator.validateIncomingData(goodName);

        Set<Item> itemSet = this.allItems.get(goodName);
        if (itemSet == null) {
            LOGGER.error("Item: {} is not being tracked and cannot be added to itemMarket!",
                    goodName);
            return;
        }
        Item item = populateItem(tickResponse);

        this.productsQuantity.merge(goodName, tickResponse.getQuantity(), Long::sum);

        LOGGER.info("Products data updated!");

        if (itemSet.contains(item)) {

            Item presentItem = itemSet.stream().filter(currentItem -> currentItem.compareTo(item) == 0).findFirst().get();
            presentItem.setQuantity(presentItem.getQuantity() + item.getQuantity());
            return;
        }
        itemSet.add(item);
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

    private OrderBookLayer.Builder populateItemLayer(long itemLeft, Item currentItem) {
        OrderBookLayer.Builder currentLayer = OrderBookLayer.newBuilder()
                .setPrice(currentItem.getPrice());

        if (currentItem.getQuantity() > itemLeft) {

            currentLayer.setQuantity(itemLeft);
        } else {

            currentLayer.setQuantity(currentItem.getQuantity());
        }
        return currentLayer;
    }

    private Item populateItem(TickResponse tickResponse) {

        Item item = new Item();
        item.setPrice(tickResponse.getPrice());
        item.setQuantity(tickResponse.getQuantity());
        item.setOrigin(tickResponse.getOrigin());

        return item;
    }

}
