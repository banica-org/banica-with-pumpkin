package com.market.banica.generator.tick;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MarketTick {
    private String origin;
    private String good;
    private long amount;
    private double price;
}
