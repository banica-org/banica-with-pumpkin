package com.market.banica.order.book.model;

import com.aurora.Aurora;
import com.google.protobuf.Any;
import com.market.Origin;
import com.market.TickResponse;
import com.market.banica.common.exception.IncorrectResponseException;
import com.orderbook.ItemOrderBookResponse;
import com.orderbook.OrderBookLayer;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ItemMarketTest {

    private static final String EGGS_ITEM_NAME = "eggs";
    private static final String RICE_ITEM_NAME = "rice";
    private static final String MEAT_ITEM_NAME = "meat";
    private static final String CHEESE_ITEM_NAME = "cheese";

    private static final String ALL_ITEMS_FIELD = "allItems";
    private static final String PRODUCTS_QUANTITY_FIELD = "productsQuantity";

    private final ItemMarket itemMarket = new ItemMarket();
    private final Map<String, TreeSet<Item>> allItems = new ConcurrentHashMap<>();
    private final Map<String, Long> productsQuantity = new ConcurrentHashMap<>();

    @Before
    public void setUp() {
        TreeSet<Item> items = this.populateItems();

        allItems.put(EGGS_ITEM_NAME, items);
        allItems.put(RICE_ITEM_NAME, new TreeSet<>());

        ReflectionTestUtils.setField(itemMarket, ALL_ITEMS_FIELD, allItems);
        ReflectionTestUtils.setField(itemMarket, PRODUCTS_QUANTITY_FIELD, productsQuantity);
    }

    @Test
    public void updateItemUpdatesProductQuantityAndAllItemsMaps() {
        //Arrange
        itemMarket.addTrackedItem(CHEESE_ITEM_NAME);
        TickResponse cheese = TickResponse.newBuilder().setGoodName(CHEESE_ITEM_NAME).setQuantity(2).setPrice(2.6).setOrigin(Origin.ASIA).build();
        Aurora.AuroraResponse build = Aurora.AuroraResponse.newBuilder().setMessage(Any.pack(cheese)).build();

        TickResponse cheese2 = TickResponse.newBuilder().setGoodName(CHEESE_ITEM_NAME).setQuantity(2).setPrice(2.6).setOrigin(Origin.ASIA).build();
        Aurora.AuroraResponse build2 = Aurora.AuroraResponse.newBuilder().setMessage(Any.pack(cheese2)).build();

        TickResponse cheese3 = TickResponse.newBuilder().setGoodName(CHEESE_ITEM_NAME).setQuantity(2).setPrice(2.6).setOrigin(Origin.AMERICA).build();
        Aurora.AuroraResponse build3 = Aurora.AuroraResponse.newBuilder().setMessage(Any.pack(cheese3)).build();

        //Act
        itemMarket.updateItem(build);
        itemMarket.updateItem(build2);
        itemMarket.updateItem(build3);

        //Assert
        assertEquals(2, allItems.get(CHEESE_ITEM_NAME).size());

        assertEquals(2, allItems.get(CHEESE_ITEM_NAME).first().getQuantity());
        assertEquals(4, allItems.get(CHEESE_ITEM_NAME).last().getQuantity());

        assertEquals(6, productsQuantity.get(CHEESE_ITEM_NAME));
        assertEquals(1, productsQuantity.size());
    }

    @Test(expected = IncorrectResponseException.class)
    public void updateItemThrowsExceptionWhenPassingRequestOfDifferentType() {
        //Arrange
        itemMarket.addTrackedItem("cheese");
        ItemOrderBookResponse response = ItemOrderBookResponse.newBuilder().build();
        Aurora.AuroraResponse build = Aurora.AuroraResponse.newBuilder().setMessage(Any.pack(response)).build();

        //Act
        itemMarket.updateItem(build);
    }

    @Test
    public void getRequestedItemReturnsListOfLayersAndUpdatesProductQuantityAndAllItemsMaps() {
        //Arrange
        itemMarket.addTrackedItem(CHEESE_ITEM_NAME);
        TickResponse cheese = TickResponse.newBuilder().setGoodName(CHEESE_ITEM_NAME).setQuantity(2).setPrice(2.6).setOrigin(Origin.ASIA).build();
        Aurora.AuroraResponse build = Aurora.AuroraResponse.newBuilder().setMessage(Any.pack(cheese)).build();

        TickResponse cheese2 = TickResponse.newBuilder().setGoodName(CHEESE_ITEM_NAME).setQuantity(2).setPrice(2.6).setOrigin(Origin.ASIA).build();
        Aurora.AuroraResponse build2 = Aurora.AuroraResponse.newBuilder().setMessage(Any.pack(cheese2)).build();

        TickResponse cheese3 = TickResponse.newBuilder().setGoodName(CHEESE_ITEM_NAME).setQuantity(2).setPrice(2.6).setOrigin(Origin.AMERICA).build();
        Aurora.AuroraResponse build3 = Aurora.AuroraResponse.newBuilder().setMessage(Any.pack(cheese3)).build();

        //Act
        itemMarket.updateItem(build);
        itemMarket.updateItem(build2);
        itemMarket.updateItem(build3);

        List<OrderBookLayer> layers = itemMarket.getRequestedItem(CHEESE_ITEM_NAME, 3);

        //Assert
        assertEquals(2, layers.size());

        assertEquals(2, layers.get(0).getQuantity());
        assertEquals(Origin.AMERICA, layers.get(0).getOrigin());

        assertEquals(1, layers.get(1).getQuantity());
        assertEquals(Origin.ASIA, layers.get(1).getOrigin());
    }

    @Test
    public void getProductsQuantityReturnsProductsQuantity() {
        //Arrange
        allItems.clear();

        //Act
        Map<String, Long> productsQuantityActual = this.itemMarket.getProductsQuantity();

        //Assert
        assertEquals(productsQuantity, productsQuantityActual);
    }

    @Test
    public void getItemSetByNameWithExistingItemNameReturnsTreeSetOfItems() {
        //Act
        Optional<Set<Item>> itemSetByName = this.itemMarket.getItemSetByName(EGGS_ITEM_NAME);

        //Assert
        assertTrue(itemSetByName.isPresent());

        Set<Item> items = itemSetByName.get();

        assertEquals(3, items.size());
        assertTrue(items.contains(new Item(1.2, 3, Origin.EUROPE)));
        assertTrue(items.contains(new Item(2.2, 1, Origin.EUROPE)));
        assertTrue(items.contains(new Item(3.2, 2, Origin.EUROPE)));
    }

    @Test
    public void getItemSetByNameWithEmptyItemMarketReturnsOptionalOfNull() {
        //Arrange
        allItems.clear();

        //Act
        Optional<Set<Item>> itemSetByName = this.itemMarket.getItemSetByName(EGGS_ITEM_NAME);

        //Assert
        assertFalse(itemSetByName.isPresent());
    }

    @Test
    public void getItemNameSetReturnsAllPresentKeys() {
        //Act
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
        assertTrue(productsQuantity.containsKey(MEAT_ITEM_NAME));
        assertEquals(0L, productsQuantity.get(MEAT_ITEM_NAME));
    }

    @Test
    public void removeUntrackedItemWithExistingItemNameRemovesItFromMap() {
        //Arrange
        this.itemMarket.addTrackedItem(MEAT_ITEM_NAME);

        //Act
        this.itemMarket.removeUntrackedItem(MEAT_ITEM_NAME);

        //Assert
        assertEquals(2, this.allItems.size());
        assertFalse(productsQuantity.containsKey(MEAT_ITEM_NAME));
    }

    private TreeSet<Item> populateItems() {
        TreeSet<Item> items = new TreeSet<>();
        items.add(new Item(1.2, 3, Origin.EUROPE));
        items.add(new Item(2.2, 1, Origin.EUROPE));
        items.add(new Item(3.2, 2, Origin.EUROPE));
        return items;
    }

}