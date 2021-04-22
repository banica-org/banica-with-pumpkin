package com.market.banica.order.book.service;

import com.market.Origin;
import com.market.banica.order.book.model.Item;
import com.market.banica.order.book.model.ItemMarket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JMXTest {

    private static final ItemMarket itemMarket = mock(ItemMarket.class);

    private static JMX jmx;

    @BeforeAll
    static void beforeAll() {

        jmx = new JMX(itemMarket);

    }

    @AfterEach
    void teardown() {

        reset(itemMarket);

    }

    @Test
    void getAllItemsByName_ProductIsTracked() {

        String itemName = "eggs";

        Set<Item> itemSet = new HashSet<>();
        Item item1 = new Item(1.2, 5, Origin.EUROPE);
        Item item2 = new Item(1.2, 5, Origin.EUROPE);
        Item item3 = new Item(1.2, 5, Origin.EUROPE);
        itemSet.add(item1);
        itemSet.add(item2);
        itemSet.add(item3);
        when(itemMarket.getItemSetByName(itemName)).thenReturn(Optional.of(itemSet));

        Set<String> expected = itemSet.stream().map(Item::toString).collect(Collectors.toSet());


        Set<String> actual = jmx.getAllItemsByName(itemName);


        assertEquals(expected, actual);
        verify(itemMarket, times(1)).getItemSetByName(itemName);

    }

    @Test
    void getAllItemsByName_ProductIsNotTracked() {

        String itemName = "eggs";
        when(itemMarket.getItemSetByName(itemName)).thenReturn(Optional.empty());

        IllegalArgumentException thrownException = assertThrows(IllegalArgumentException.class,
                () -> jmx.getAllItemsByName(itemName));

        verify(itemMarket, times(1)).getItemSetByName(itemName);
        assertEquals("Product: " + itemName + " is not tracked!", thrownException.getMessage());

    }

    @Test
    void getProductsQuantity() {

        Map<String, Long> productsQuantity = new HashMap<>();
        productsQuantity.put("flour", 0L);
        productsQuantity.put("eggs", 5L);
        when(itemMarket.getProductsQuantity()).thenReturn(productsQuantity);

        Map<String, Long> productsQuantityActual = jmx.getProductsQuantity();

        assertEquals(productsQuantity, productsQuantityActual);
        verify(itemMarket, times(1)).getProductsQuantity();

    }

}