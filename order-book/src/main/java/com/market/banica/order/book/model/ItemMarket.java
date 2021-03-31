package com.market.banica.order.book.model;

import com.aurora.Aurora;
import com.google.protobuf.InvalidProtocolBufferException;
import com.market.Origin;
import com.market.TickResponse;
import com.market.banica.order.book.exception.IncorrectResponseException;
import com.orderbook.ItemOrderBookRequest;
import com.orderbook.OrderBookLayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
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

    private final Map<String, TreeSet<Item>> allItems;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private static final Logger LOGGER = LogManager.getLogger(ItemMarket.class);

    @Autowired
    public ItemMarket() {
        allItems = new ConcurrentHashMap<>();
        addDummyData();
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
        TreeSet<Item> items = this.allItems.get(itemName);

        if (items == null) {
            return new ArrayList<>();
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

    private void addDummyData() {

        TreeSet<Item> cheeseItems = new TreeSet<>();
        cheeseItems.add(new Item(2.6, 2, Origin.AMERICA));
        cheeseItems.add(new Item(4.0, 2, Origin.ASIA));
        cheeseItems.add(new Item(4.0, 5, Origin.EUROPE));
        cheeseItems.add(new Item(4.1, 2, Origin.ASIA));
        allItems.put("cheese", cheeseItems);

        TreeSet<Item> cocoaItems = new TreeSet<>();

        cocoaItems.add(new Item(1.6, 3, Origin.ASIA));
        cocoaItems.add(new Item(1.5, 4, Origin.AMERICA));
        cocoaItems.add(new Item(1.7, 1, Origin.EUROPE));
        allItems.put("cocoa", cocoaItems);

    }

}
