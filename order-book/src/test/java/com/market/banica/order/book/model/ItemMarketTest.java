package com.market.banica.order.book.model;

import com.market.Origin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemMarketTest {
    private static final String EGGS_ITEM_NAME = "eggs";
    private static final String RICE_ITEM_NAME = "rice";
    private static final String MEAT_ITEM_NAME = "meat";

    private final ItemMarket itemMarket = new ItemMarket();
    private final Map<String, TreeSet<Item>> allItems = new ConcurrentHashMap<>();

    @BeforeEach
    public void setUp() {
        TreeSet<Item> items = new TreeSet<>();
        items.add(new Item(1.2, 3, Origin.EUROPE));
        items.add(new Item(2.2, 1, Origin.EUROPE));
        items.add(new Item(3.2, 2, Origin.EUROPE));
        allItems.put(EGGS_ITEM_NAME, items);
        allItems.put(RICE_ITEM_NAME, new TreeSet<>());

        ReflectionTestUtils.setField(itemMarket, "allItems", allItems);
    }

    @Test
    public void getItemSetByNameWithExistingItemNameReturnsTreeSetOfItems() {
        //Arrange, Act
        Optional<Set<Item>> itemSetByName = this.itemMarket.getItemSetByName(EGGS_ITEM_NAME);
        Set<Item> items = itemSetByName.get();

        //Assert
        assertEquals(3, items.size());
        assertTrue(items.contains(new Item(1.2, 3, Origin.EUROPE)));
        assertTrue(items.contains(new Item(2.2, 1, Origin.EUROPE)));
        assertTrue(items.contains(new Item(3.2, 2, Origin.EUROPE)));
    }

    @Test
    public void getItemNameSetReturnsAllPresentKeys() {
        //Arrange, Act
        Set<String> itemNameSet = this.itemMarket.getItemNameSet();

        //Assert
        assertTrue(itemNameSet.contains(EGGS_ITEM_NAME));
        assertTrue(itemNameSet.contains(RICE_ITEM_NAME));
    }

    @Test
    public void addTrackedItemWithItemNameAddsNewTreeSetAsValue() {
        //Arrange
        this.itemMarket.addTrackedItem(MEAT_ITEM_NAME);

        //Act
        Set<Item> items = this.itemMarket.getItemSetByName(MEAT_ITEM_NAME).get();

        //Assert
        assertTrue(items.isEmpty());
    }

    @Test
    public void removeUntrackedItemWithExistingItemNameRemovesItFromMap() {
        //Arrange
        this.itemMarket.addTrackedItem(MEAT_ITEM_NAME);

        //Act
        this.itemMarket.removeUntrackedItem(MEAT_ITEM_NAME);

        //Assert
        assertEquals(2, this.allItems.size());
    }


}