package com.market.banica.generator.model;


import com.market.Origin;
import com.market.TickResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString
public class MarketTick {

    private final String origin;
    private final String good;
    private final long amount;
    private final double price;

    public TickResponse toResponse() {
        return TickResponse.newBuilder()
                    .setOrigin(Origin.valueOf(origin.toUpperCase()))
                    .setTimestamp(System.currentTimeMillis())
                    .setGoodName(good)
                    .setPrice(price)
                    .setQuantity(amount)
                    .build();
    }
}
