package com.market.banica.order.book.model;

import com.aurora.Aurora;
import com.google.protobuf.InvalidProtocolBufferException;
import com.market.Origin;
import com.market.TickResponse;
import com.market.banica.order.book.exception.IncorrectResponseException;
import com.orderbook.OrderBookLayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
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
        allItems = new ConcurrentHashMap<>();
        productsQuantity = new ConcurrentHashMap<>();
        addDummyData2();
    }

    public Optional<Set<Item>> getItemSetByName(String itemName) {
        return Optional.of(allItems.get(itemName));
    }

    public Set<String> getItemNameSet() {
        return allItems.keySet();
    }

    public void addTrackedItem(String itemName) {
        allItems.put(itemName, new TreeSet<>());
    }

    public void removeUntrackedItem(String itemName) {
        allItems.remove(itemName);
    }

    public void updateItem(Aurora.AuroraResponse response) {
        if (response.getMessage().is(TickResponse.class)) {
            TickResponse tickResponse;

            try {
                tickResponse = response.getMessage().unpack(TickResponse.class);
            } catch (InvalidProtocolBufferException e) {
                throw new IncorrectResponseException("Response is not correct!");
            }

            Item item = new Item();
            item.setPrice(tickResponse.getPrice());
            item.setQuantity(tickResponse.getQuantity());
            item.setOrigin(tickResponse.getOrigin());

            productsQuantity.merge(tickResponse.getGoodName(), tickResponse.getQuantity(), Long::sum);

            Set<Item> itemSet = allItems.get(tickResponse.getGoodName());
            if (itemSet != null) {
                if (itemSet.contains(item)) {
                    Item presentItem = itemSet.stream().filter(item1 -> Double.compare(item1.getPrice(), item.getPrice()) == 0
                            && item1.getOrigin().equals(item.getOrigin())).findFirst().get();
                    presentItem.setQuantity(presentItem.getQuantity() + item.getQuantity());
                    return;
                }

                itemSet.add(item);
            } else {
                LOGGER.error("Item: {} is not being tracked and cannot be added to itemMarket!",
                        tickResponse.getGoodName());
            }

            LOGGER.info("Products data updated!");
        } else {
            throw new IncorrectResponseException("Response is not correct!");
        }
    }

    public List<OrderBookLayer> getRequestedItem(String itemName, long quantity) {
        LOGGER.info("Getting requested item: {} with quantity: {}", itemName, quantity);
        TreeSet<Item> items = this.allItems.get(itemName);

        if (items == null || productsQuantity.get(itemName) < quantity) {
            return Collections.emptyList();
        }

        List<OrderBookLayer> layers;
        try {
            lock.writeLock().lock();

            layers = new ArrayList<>();

            Iterator<Item> iterator = items.iterator();
            long itemLeft = quantity;

            while (iterator.hasNext() && itemLeft > 0) {
                Item currentItem = iterator.next();

                OrderBookLayer.Builder currentLayer = OrderBookLayer.newBuilder()
                        .setPrice(currentItem.getPrice());

                if (currentItem.getQuantity() >= itemLeft) {
                    currentLayer.setQuantity(itemLeft);

                    if (currentItem.getQuantity() == itemLeft) {
                        iterator.remove();
                    }

                    currentItem.setQuantity(currentItem.getQuantity() - itemLeft);
                } else if (currentItem.getQuantity() < itemLeft) {
                    currentLayer.setQuantity(currentItem.getQuantity());

                    iterator.remove();
                }
                productsQuantity.put(itemName, productsQuantity.get(itemName) - currentLayer.getQuantity());
                itemLeft -= currentLayer.getQuantity();

                OrderBookLayer build = currentLayer
                        .setOrigin(currentItem.getOrigin())
                        .build();
                layers.add(build);
            }
        } finally {
            lock.writeLock().unlock();
        }
        return layers;
    }


    private void addDummyData2() {
        fillMapsWithDummyData("banica", 156000.0, 2);
        fillMapsWithDummyData("pumpkin", 0.003, 300);
        fillMapsWithDummyData("milk", 1.0, 2);
        fillMapsWithDummyData("crusts", 0.0001, 200);
        fillMapsWithDummyData("water", 0.01, 300);
        fillMapsWithDummyData("eggs", 0.01, 12);
        fillMapsWithDummyData("sauce", 4.0, 150);
        fillMapsWithDummyData("sugar", 0.02, 50);
        fillMapsWithDummyData("ketchup", 0.03, 50);
        fillMapsWithDummyData("tomatoes", 0.01, 65);
    }

    private void fillMapsWithDummyData(String productName, double price, int quantity) {
        TreeSet<Item> objects = new TreeSet<>();
        Item item = new Item(price, quantity, getRandomOrigin());
        objects.add(item);
        allItems.put(productName, objects);
        productsQuantity.merge(productName, item.getQuantity(), Long::sum);
    }

    private Origin getRandomOrigin() {
        return Origin.forNumber(new Random().nextInt(3) + 1);
    }


}
