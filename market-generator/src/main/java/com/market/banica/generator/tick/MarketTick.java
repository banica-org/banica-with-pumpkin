package com.market.banica.generator.tick;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MarketTick {
    private final String origin;
    private final String good;
    private final long amount;
    private final double price;
}