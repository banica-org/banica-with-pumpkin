package com.market.banica.order.book.model;

import com.market.Origin;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class Item implements Comparable<Item> {

    private double price;
    private long quantity;
    private Origin origin;

    @Override
    public int compareTo(Item other) {
        int result = Double.compare(this.price, other.price);
        if (result == 0) {
            result = this.origin.compareTo(other.origin);
        }
        return result;
    }

}
