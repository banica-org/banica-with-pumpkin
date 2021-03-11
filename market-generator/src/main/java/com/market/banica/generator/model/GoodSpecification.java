package com.market.banica.generator.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class GoodSpecification {
    private final long quantityLow;
    private final long quantityHigh;
    private final long quantityStep;
    private final double priceLow;
    private final double priceHigh;
    private final double priceStep;
    private final int periodLow;
    private final int periodHigh;
    private final int periodStep;
}
