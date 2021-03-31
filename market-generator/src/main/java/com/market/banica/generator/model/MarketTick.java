package com.market.banica.generator.model;

import com.market.Origin;
import lombok.Getter;
import lombok.ToString;
import org.springframework.beans.factory.annotation.Value;

import java.util.Date;
import java.util.Locale;

@Getter
@ToString
public class MarketTick {

    private static Origin origin;
    private final String good;
    private final long quantity;
    private final double price;
    private final Date date;

    public MarketTick(String good, long amount, double price, Date date) {
        this.good = good;
        this.quantity = amount;
        this.price = price;
        this.date = date;
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

}