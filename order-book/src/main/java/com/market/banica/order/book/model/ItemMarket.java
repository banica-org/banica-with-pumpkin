package com.market.banica.order.book.model;

import com.aurora.Aurora;
import com.google.gson.Gson;
import com.google.protobuf.InvalidProtocolBufferException;
import com.market.Origin;
import com.market.TickResponse;
import com.market.banica.order.book.exception.IncorrectResponseException;
import com.orderbook.OrderBookLayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

@Component
public class ItemMarket {

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<String, TreeSet<Item>> allItems;
    private final Map<String, Long> productsQuantity;
    private Set<String> subscribedItems;

    private static final String FILE_PATH = "C:\\Programs\\banica\\banica-with-pumpkin\\order-book\\src\\main\\java\\com\\market\\banica\\order\\book\\subscribedProductsBackUp.json";
    private static final Logger LOGGER = LogManager.getLogger(ItemMarket.class);

    @Autowired
    public ItemMarket() {
        this.allItems = new ConcurrentHashMap<>();
        this.productsQuantity = new ConcurrentHashMap<>();
        this.subscribedItems = new HashSet<>();
        addDummyData();
//        subscribedItems.add("test1");
//        subscribedItems.add("test2");
    }

    public Optional<Set<Item>> getItemSetByName(String itemName) {
        return Optional.of(this.allItems.get(itemName));
    }

    public Set<String> getItemNameSet() {
        return this.allItems.keySet();
    }

    public void addTrackedItem(String itemName) {
        this.allItems.put(itemName, new TreeSet<>());
    }

    public void removeUntrackedItem(String itemName) {
        this.allItems.remove(itemName);
    }

    public void updateItem(Aurora.AuroraResponse response) {

        if (!response.getMessage().is(TickResponse.class)) {
            throw new IncorrectResponseException("Response is not correct!");
        }

        TickResponse tickResponse;

        try {
            tickResponse = response.getMessage().unpack(TickResponse.class);
        } catch (InvalidProtocolBufferException e) {
            throw new IncorrectResponseException("Response is not correct!");
        }
        Set<Item> itemSet = this.allItems.get(tickResponse.getGoodName());
        if (itemSet == null) {
            LOGGER.error("Item: {} is not being tracked and cannot be added to itemMarket!",
                    tickResponse.getGoodName());
            return;
        }
        Item item = populateItem(tickResponse);

        this.productsQuantity.merge(tickResponse.getGoodName(), tickResponse.getQuantity(), Long::sum);

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
        TreeSet<Item> items = this.allItems.get(itemName);

        if (items == null || this.productsQuantity.get(itemName) < quantity) {

            return Collections.emptyList();
        }

        List<OrderBookLayer> layers;
        lock.writeLock().lock();

        try {
            layers = new ArrayList<>();

            Iterator<Item> iterator = items.iterator();
            long itemLeft = quantity;

            while (itemLeft > 0) {
                Item currentItem = iterator.next();

                OrderBookLayer.Builder currentLayer = populateItemLayer(iterator, itemLeft, currentItem);

                this.productsQuantity.put(itemName, this.productsQuantity.get(itemName) - currentLayer.getQuantity());
                itemLeft -= currentLayer.getQuantity();

                OrderBookLayer orderBookLayer = currentLayer
                        .setOrigin(currentItem.getOrigin())
                        .build();
                layers.add(orderBookLayer);
            }
        } finally {
            lock.writeLock().unlock();
        }
        return layers;
    }

    public void persistItemInFileBackUp(String itemName) {
        this.lock.writeLock().lock();

        try {
            modifyFile(itemName, subscribedItems::add);
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    public void removeItemFromFileBackUp(String itemName) {
        this.lock.writeLock().lock();

        try {
            modifyFile(itemName, subscribedItems::remove);
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    private void modifyFile(String itemName, Consumer<String> consumer) {
        Gson gson = new Gson();
        consumer.accept(itemName);

        try (FileWriter writer = new FileWriter(FILE_PATH);) {
            gson.toJson(subscribedItems, writer);
            writer.flush();
        } catch (IOException e) {
            LOGGER.error("An error occurred while modifying data in backup file: {}", e.getMessage());
        }
    }

    @PostConstruct
    public void readBackUp() {
        this.lock.readLock().lock();

        try {
            Gson gson = new Gson();
            try (FileReader reader = new FileReader(FILE_PATH);) {
                this.subscribedItems = gson.fromJson(reader, Set.class);
                if (subscribedItems == null) {
                    subscribedItems = new HashSet<>();
                }

            } catch (IOException e) {
                LOGGER.error("An error occurred while reading data from backup file: {}", e.getMessage());
            }
        } finally {
            this.lock.readLock().unlock();
        }
    }

    public Set<String> getSubscribedItems() {
        return this.subscribedItems;
    }

    private OrderBookLayer.Builder populateItemLayer(Iterator<Item> iterator, long itemLeft, Item currentItem) {
        OrderBookLayer.Builder currentLayer = OrderBookLayer.newBuilder()
                .setPrice(currentItem.getPrice());

        if (currentItem.getQuantity() > itemLeft) {

            currentLayer.setQuantity(itemLeft);
            currentItem.setQuantity(currentItem.getQuantity() - itemLeft);

        } else if (currentItem.getQuantity() <= itemLeft) {

            currentLayer.setQuantity(currentItem.getQuantity());
            iterator.remove();
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

    private void addDummyData() {

        TreeSet<Item> cheeseItems = new TreeSet<>();
        cheeseItems.add(new Item(2.6, 2, Origin.AMERICA));
        cheeseItems.add(new Item(2.6, 2, Origin.AMERICA));
        cheeseItems.add(new Item(4.0, 5, Origin.EUROPE));
        cheeseItems.add(new Item(4.1, 2, Origin.ASIA));
        this.allItems.put("cheese", cheeseItems);
        for (Item cheeseItem : cheeseItems) {
            this.productsQuantity.merge("cheese", cheeseItem.getQuantity(), Long::sum);
        }

        TreeSet<Item> cocoaItems = new TreeSet<>();

        cocoaItems.add(new Item(1.6, 3, Origin.ASIA));
        cocoaItems.add(new Item(1.5, 4, Origin.AMERICA));
        cocoaItems.add(new Item(1.7, 1, Origin.EUROPE));
        this.allItems.put("cocoa", cocoaItems);
        for (Item cocoaItem : cocoaItems) {
            this.productsQuantity.merge("cocoa", cocoaItem.getQuantity(), Long::sum);
        }
    }
}
