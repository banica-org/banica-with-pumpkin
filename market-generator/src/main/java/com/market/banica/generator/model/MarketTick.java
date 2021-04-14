package com.market.banica.generator.model;

import com.market.Origin;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.beans.factory.annotation.Value;

import java.util.Locale;

@Getter
@ToString
@NoArgsConstructor
@EqualsAndHashCode
public class MarketTick implements Comparable<MarketTick> {

    private static Origin origin;
    private String good;
    private long quantity;
    private double price;
    private long timestamp;

    public MarketTick(String good, long amount, double price, long timestamp) {
        this.good = good;
        this.quantity = amount;
        this.price = price;
        this.timestamp = timestamp;
    }

    public static Origin getOrigin() {
        return origin;
    }

    public static void setOrigin(@Value("${market.name}") String marketName) {
        origin = Origin.UNSPECIFIED;
        for (Origin value : Origin.values()) {
            if (value.toString().equals(marketName.toUpperCase(Locale.ROOT))) {
                origin = value;
                break;
            }
        }
    }

    @Override
    public int compareTo(MarketTick other) {
        return Long.compare(this.timestamp, other.timestamp);
    }

}