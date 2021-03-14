package com.market.banica.order.book;

import java.util.List;

public class Item implements Comparable<Item> {

    private double price;
    private int quantity;
    private List<ItemID> itemIDs;

    public static class ItemID {

        private String id;
        private String location;

    }

    @Override
    public int compareTo(Item other) {
        return Double.compare(this.price, other.price);
    }

}
