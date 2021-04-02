package com.market.banica.generator.service;

import com.market.TickResponse;
import com.market.banica.generator.model.MarketTick;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

public interface MarketState {
    void addTickToMarketState(MarketTick marketTick);

    List<TickResponse> generateMarketTicks(String topic);

    Map<String, Set<MarketTick>> getMarketState();
}
