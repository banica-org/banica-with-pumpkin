package com.market.banica.order.book;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ItemMarket {

    private final Map<String, TreeSet<Item>> allItems;

    public ItemMarket() {
        allItems = new ConcurrentHashMap<>();
//        addDummyData();
    }

    public Map<String, TreeSet<Item>> getAllItems() {
        return allItems;
    }

    public Set<Item> getAllItemsByName(String itemName) {
        return allItems.get(itemName) == null ? Collections.emptySet() : allItems.get(itemName);
    }

//    private void addDummyData() {
//
//        TreeSet<Item> cheeseItems = new TreeSet<>();
//
//        ArrayList<Item.ItemID> item1 = new ArrayList<>();
//        item1.add(new Item.ItemID("1", "Asia"));
//        item1.add(new Item.ItemID("2", "Europe"));
//        item1.add(new Item.ItemID("3", "Asia"));
//
//        ArrayList<Item.ItemID> item2 = new ArrayList<>();
//        item2.add(new Item.ItemID("4", "Americas"));
//        item2.add(new Item.ItemID("5", "Europe"));
//        item2.add(new Item.ItemID("6", "Americas"));
//        item2.add(new Item.ItemID("7", "Asia"));
//
//        cheeseItems.add(new Item(2.6, 3, item1));
//        cheeseItems.add(new Item(2.5, 4, item2));
//        cheeseItems.add(new Item(2.7, 1, Collections.singletonList(new Item.ItemID("8", "Americas"))));
//        allItems.put("cheese", cheeseItems);
//
//        TreeSet<Item> cocoaItems = new TreeSet<>();
//
//        ArrayList<Item.ItemID> item3 = new ArrayList<>();
//        item3.add(new Item.ItemID("1", "Asia"));
//        item3.add(new Item.ItemID("2", "Europe"));
//        item3.add(new Item.ItemID("3", "Asia"));
//
//        ArrayList<Item.ItemID> item4 = new ArrayList<>();
//        item4.add(new Item.ItemID("4", "Americas"));
//        item4.add(new Item.ItemID("5", "Europe"));
//        item4.add(new Item.ItemID("6", "Americas"));
//        item4.add(new Item.ItemID("7", "Asia"));
//
//        cocoaItems.add(new Item(1.6, 3, item3));
//        cocoaItems.add(new Item(1.5, 4, item4));
//        cocoaItems.add(new Item(1.7, 1, Collections.singletonList(new Item.ItemID("8", "Americas"))));
//        allItems.put("cocoa", cocoaItems);
//
//    }

    public Set<String> getAllProductNames(){
        return allItems.keySet();
    }

}
