package com.market.banica.generator.model;

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

    public GoodSpecification(long quantityLow, long quantityHigh, long quantityStep, double priceLow, double priceHigh, double priceStep, int periodLow, int periodHigh, int periodStep) {
        this.quantityLow = quantityLow;
        this.quantityHigh = quantityHigh;
        this.quantityStep = quantityStep;
        this.priceLow = priceLow;
        this.priceHigh = priceHigh;
        this.priceStep = priceStep;
        this.periodLow = periodLow;
        this.periodHigh = periodHigh;
        this.periodStep = periodStep;
    }
}
