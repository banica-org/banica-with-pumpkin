package com.market.banica.generator.configuration;

public interface MarketConfiguration {
    void addGoodSpecification(String origin, String good,
                              long quantityLow, long quantityHigh, long quantityStep,
                              double priceLow, double priceHigh, double priceStep,
                              int periodLow, int periodHigh, int periodStep);

    void removeGoodSpecification(String origin, String good);

    void updateGoodSpecification(String origin, String good,
                                 long quantityLow, long quantityHigh, long quantityStep,
                                 double priceLow, double priceHigh, double priceStep,
                                 int periodLow, int periodHigh, int periodStep);
}
