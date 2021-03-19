package com.market.banica.order.book;


import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class Item implements Comparable<Item> {

    private double price;
    private int quantity;
    private List<ItemID> itemIDs;

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    public static class ItemID {

        private String id;
        private String location;

    }

    @Override
    public int compareTo(Item other) {
        return Double.compare(this.price, other.price);
    }

}
