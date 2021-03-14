package com.market.banica.order.book;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Item implements Comparable<Item> {

    private double price;
    private int quantity;
    private List<ItemID> itemIDs;

    public static class ItemID {

        public ItemID(String id, String location) {
            this.id = id;
            this.location = location;
        }

        private String id;
        private String location;

    }

    @Override
    public int compareTo(Item other) {
        return Double.compare(this.price, other.price);
    }

}
