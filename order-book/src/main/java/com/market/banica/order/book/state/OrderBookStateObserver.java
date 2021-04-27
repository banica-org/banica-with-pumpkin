package com.market.banica.order.book.state;

import com.market.banica.order.book.model.Item;
import com.market.banica.order.book.model.ItemMarket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@EnableMBeanExport
@ManagedResource
@Service
public class OrderBookStateObserver {

    private final ItemMarket itemMarket;

    @Autowired
    public OrderBookStateObserver(ItemMarket itemMarket) {
        this.itemMarket = itemMarket;
    }

    @ManagedOperation
    public Set<String> getAllItemsByName(String itemName) {
        return itemMarket.getItemSetByName(itemName)
                .orElseThrow(() -> new IllegalArgumentException("Product: " + itemName + " is not tracked!"))
                .stream()
                .map(Item::toString)
                .collect(Collectors.toSet());
    }

    @ManagedOperation
    public Map<String, Long> getProductsQuantity() {
        return itemMarket.getProductsQuantity();
    }

}