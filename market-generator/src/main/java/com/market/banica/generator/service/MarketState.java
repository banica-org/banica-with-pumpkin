package com.market.banica.generator.service;

import com.market.TickResponse;

import java.util.List;
import java.util.Map;

public interface MarketState {
    void addGoodToMarketState(String goodName, double goodPrice, long goodQuantity);

    void updateSubscribers(String itemName, long itemQuantity, double itemPrice);

    List<TickResponse> generateMarketTicks(String topic);

    Map<String, Map<Double, Long>> getMarketState();
}
