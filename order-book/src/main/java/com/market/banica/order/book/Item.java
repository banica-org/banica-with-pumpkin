package com.market.banica.order.book;


import com.market.Origin;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class Item implements Comparable<Item> {

    private double price;
    private int quantity;
    private Origin origin;

    @Override
    public int compareTo(Item other) {
        return Double.compare(this.price, other.price);
    }

}
