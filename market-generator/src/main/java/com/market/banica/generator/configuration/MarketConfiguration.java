package com.market.banica.generator.configuration;

public interface MarketConfiguration {

    void addGoodSpecification(String good, long quantityLow, long quantityHigh, long quantityStep,
                              double priceLow, double priceHigh, double priceStep,
                              int periodLow, int periodHigh, int periodStep);

    void removeGoodSpecification(String good);

    void updateGoodSpecification(String good, long quantityLow, long quantityHigh, long quantityStep,
                                 double priceLow, double priceHigh, double priceStep,
                                 int periodLow, int periodHigh, int periodStep);

    void setPersistenceFrequencyInSeconds(int frequency);

}
