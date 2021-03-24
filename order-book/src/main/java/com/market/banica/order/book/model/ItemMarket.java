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
        //addDummyData();
    }

    public Optional<Set<Item>> getItemSetByName(String itemName) {
        return Optional.ofNullable(allItems.get(itemName));
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

        TreeSet<Item> banicaItems = new TreeSet<>();
        banicaItems.add(new Item(2.6, 3, Origin.AMERICA));
        banicaItems.add(new Item(2.5, 4, Origin.EUROPE));
        banicaItems.add(new Item(2.7, 1, Origin.ASIA));
        allItems.put("banica", banicaItems);

        TreeSet<Item> pumpkinItems = new TreeSet<>();
        pumpkinItems.add(new Item(1.6, 3, Origin.ASIA));
        pumpkinItems.add(new Item(1.5, 4, Origin.AMERICA));
        pumpkinItems.add(new Item(1.7, 1, Origin.EUROPE));
        allItems.put("pumpkin", pumpkinItems);

        TreeSet<Item> milkItems = new TreeSet<>();
        milkItems.add(new Item(2.6, 3, Origin.AMERICA));
        milkItems.add(new Item(2.5, 4, Origin.EUROPE));
        milkItems.add(new Item(2.7, 1, Origin.ASIA));
        allItems.put("milk", milkItems);

        TreeSet<Item> crustItems = new TreeSet<>();
        crustItems.add(new Item(2.6, 3, Origin.AMERICA));
        crustItems.add(new Item(2.5, 4, Origin.EUROPE));
        crustItems.add(new Item(2.7, 1, Origin.ASIA));
        allItems.put("crusts", crustItems);

        TreeSet<Item> waterItems = new TreeSet<>();
        waterItems.add(new Item(2.6, 3, Origin.AMERICA));
        waterItems.add(new Item(2.5, 4, Origin.EUROPE));
        waterItems.add(new Item(2.7, 1, Origin.ASIA));
        allItems.put("water", waterItems);

        TreeSet<Item> eggItems = new TreeSet<>();
        eggItems.add(new Item(2.6, 3, Origin.AMERICA));
        eggItems.add(new Item(2.5, 4, Origin.EUROPE));
        eggItems.add(new Item(2.7, 1, Origin.ASIA));
        allItems.put("eggs", eggItems);

        TreeSet<Item> sauceItems = new TreeSet<>();
        sauceItems.add(new Item(2.6, 3, Origin.AMERICA));
        sauceItems.add(new Item(2.5, 4, Origin.EUROPE));
        sauceItems.add(new Item(2.7, 1, Origin.ASIA));
        allItems.put("sauce", sauceItems);

        TreeSet<Item> sugarItems = new TreeSet<>();
        sugarItems.add(new Item(2.6, 3, Origin.AMERICA));
        sugarItems.add(new Item(2.5, 4, Origin.EUROPE));
        sugarItems.add(new Item(2.7, 1, Origin.ASIA));
        allItems.put("sugar", sugarItems);

        TreeSet<Item> ketchupItems = new TreeSet<>();
        ketchupItems.add(new Item(2.6, 3, Origin.AMERICA));
        ketchupItems.add(new Item(2.5, 4, Origin.EUROPE));
        ketchupItems.add(new Item(2.7, 1, Origin.ASIA));
        allItems.put("ketchup", ketchupItems);

        TreeSet<Item> tomatoeItems = new TreeSet<>();
        tomatoeItems.add(new Item(2.6, 3, Origin.AMERICA));
        tomatoeItems.add(new Item(2.5, 4, Origin.EUROPE));
        tomatoeItems.add(new Item(2.7, 1, Origin.ASIA));
        allItems.put("tomatoes", tomatoeItems);

    }

}
