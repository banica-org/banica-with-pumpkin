package com.market.banica.generator.service;

import com.market.TickResponse;
import com.market.banica.generator.model.MarketTick;
import com.market.banica.generator.util.PersistScheduler;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface MarketState {

    void addTickToMarketSnapshot(MarketTick marketTick);

    List<TickResponse> generateMarketTicks(String good);

    PersistScheduler getPersistScheduler();

    MarketTick removeItemFromState(String itemName, long itemQuantity, double itemPrice);

    void addGoodToState(String itemName, double itemPrice, long itemQuantity, long timestamp);

    Map<String, Set<MarketTick>> getMarketState();
}