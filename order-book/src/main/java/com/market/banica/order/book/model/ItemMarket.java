package com.market.banica.order.book.model;

import com.aurora.Aurora;
import com.google.gson.Gson;
import com.google.protobuf.InvalidProtocolBufferException;
import com.market.Origin;
import com.market.TickResponse;

import com.market.banica.common.exception.IncorrectResponseException;
import com.market.banica.common.util.ApplicationDirectoryUtil;
import com.market.banica.common.validator.DataValidator;
import com.orderbook.OrderBookLayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
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
@ManagedResource
@EnableMBeanExport
public class ItemMarket {

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<String, TreeSet<Item>> allItems;
    private final Map<String, Long> productsQuantity;
    private Set<String> subscribedItems;

    private final String FILE_PATH;
    private static final Logger LOGGER = LogManager.getLogger(ItemMarket.class);

    @Autowired
    public ItemMarket(@Value("${backup.url}") String fileName) {
        this.allItems = new ConcurrentHashMap<>();
        this.productsQuantity = new ConcurrentHashMap<>();
        this.subscribedItems = new HashSet<>();
        FILE_PATH = fileName;
        addDummyData();
        print(productsQuantity);
    }

    public Optional<Set<Item>> getItemSetByName(String itemName) {
        return Optional.of(this.allItems.get(itemName));
    }

    public Set<String> getItemNameSet() {
        return this.allItems.keySet();
    }

    public void addTrackedItem(String itemName) {
//        this.allItems.put(itemName, new TreeSet<>());
        this.allItems.putIfAbsent(itemName, new TreeSet<>());
        this.productsQuantity.putIfAbsent(itemName, 0L);
    }

    private void print(Map<String, Long> productsQuantity) {
        System.out.println("Before");
        for (Map.Entry<String, Long> entry : productsQuantity.entrySet()) {
            System.out.println(entry);
        }

    }

    @ManagedOperation
    public void printBeforeDestroy() {
        System.out.println("After");
        for (Map.Entry<String, Long> entry : productsQuantity.entrySet()) {
            System.out.println(entry);
        }
    }

    public void removeUntrackedItem(String itemName) {
        this.allItems.remove(itemName);
    }

    private void addDummyData() {
        //        extracted("banica", 1.0, 7, Origin.EUROPE);
        extracted("banica", 7.0, 7, Origin.EUROPE);
//        extracted("crusts", 1.0, 500, Origin.EUROPE);
        extracted("crusts", 1.0, 20, Origin.EUROPE);
        extracted("eggs", 1.0, 20, Origin.EUROPE);
        extracted("eggs", 1.0, 500, Origin.EUROPE);


        extracted("cheese", 9999.9, 5, Origin.EUROPE);
//        extracted("eggs", 5.0, 20, Origin.EUROPE);
//        extracted("eggs", 5.0, 8, Origin.EUROPE);
//        extracted("eggs", 5.0, 8, Origin.AMERICA);
        extracted("water", 5.0, 400, Origin.EUROPE);
        extracted("tomatoes", 5.0, 70, Origin.EUROPE);
        extracted("milk", 5.0, 3, Origin.EUROPE);
        extracted("pumpkin", 5.0, 400, Origin.EUROPE);
        extracted("sugar", 5.0, 60, Origin.EUROPE);
    }

    private void extracted(String product, double price, long quantity, Origin origin) {
        this.allItems.putIfAbsent(product, new TreeSet<>());

        Item item = new Item(price, quantity, origin);
        Set<Item> newItemSet = this.allItems.get(product);

        this.productsQuantity.merge(product, quantity, Long::sum);
        if (newItemSet.contains(item)) {
            Item presentItem = newItemSet.stream().filter(currentItem -> currentItem.compareTo(item) == 0).findFirst().get();
            presentItem.setQuantity(presentItem.getQuantity() + item.getQuantity());
            return;
        }
        newItemSet.add(item);
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
        System.out.println("tick resp -> "+tickResponse.getGoodName() + " : " + tickResponse.getQuantity() );
        Set<Item> itemSet = this.allItems.get(tickResponse.getGoodName());
        if (itemSet == null) {
            LOGGER.error("Item: {} is not being tracked and cannot be added to itemMarket!",
                    tickResponse.getGoodName());
            return;
        }
        Item item = populateItem(tickResponse);

        this.productsQuantity.merge(tickResponse.getGoodName(), tickResponse.getQuantity(), Long::sum);

//        LOGGER.info("Products data updated!");
        if (itemSet.contains(item)) {
            Item presentItem = itemSet.stream().filter(currentItem -> currentItem.compareTo(item) == 0).findFirst().get();
            presentItem.setQuantity(presentItem.getQuantity() + item.getQuantity());
            return;
        }
        itemSet.add(item);
    }

    public List<OrderBookLayer> getRequestedItem(String itemName, long quantity) {

//        LOGGER.info("Getting requested item: {} with quantity: {}", itemName, quantity);
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

    public void persistItemInFileBackUp(String itemName) {
        this.lock.writeLock().lock();

        try {
            modifyFile(itemName, subscribedItems::add);
        } catch (IOException e) {
            LOGGER.info("Problem occur while modifying cable");
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    public void removeItemFromFileBackUp(String itemName) throws IOException {
        this.lock.writeLock().lock();

        try {
            modifyFile(itemName, subscribedItems::remove);
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    private void modifyFile(String itemName, Consumer<String> consumer) throws IOException {
        Gson gson = new Gson();
        consumer.accept(itemName);

        try (FileWriter writer = new FileWriter(ApplicationDirectoryUtil.getConfigFile(FILE_PATH));) {
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
            try (FileReader reader = new FileReader(ApplicationDirectoryUtil.getConfigFile(FILE_PATH));) {
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

    private OrderBookLayer.Builder populateItemLayer(long itemLeft, Item currentItem) {
        OrderBookLayer.Builder currentLayer = OrderBookLayer.newBuilder()
                .setPrice(currentItem.getPrice());

        if (currentItem.getQuantity() > itemLeft) {

            currentLayer.setQuantity(itemLeft);
        } else if (currentItem.getQuantity() <= itemLeft) {

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
