package com.market.banica.order.book.model;

import com.market.Origin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ItemMarket {

    private final Map<String, TreeSet<Item>> allItems;

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

    private void addDummyData() {

        TreeSet<Item> cheeseItems = new TreeSet<>();

        cheeseItems.add(new Item(2.6, 3, Origin.AMERICA));
        cheeseItems.add(new Item(2.5, 4, Origin.EUROPE));
        cheeseItems.add(new Item(2.7, 1, Origin.ASIA));
        allItems.put("cheese", cheeseItems);

        TreeSet<Item> cocoaItems = new TreeSet<>();

        cocoaItems.add(new Item(1.6, 3, Origin.ASIA));
        cocoaItems.add(new Item(1.5, 4, Origin.AMERICA));
        cocoaItems.add(new Item(1.7, 1, Origin.EUROPE));
        allItems.put("cocoa", cocoaItems);

    }

}
