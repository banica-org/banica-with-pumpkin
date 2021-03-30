package com.market.banica.generator.service.snapshot;

import com.market.banica.generator.model.MarketTick;

import java.util.Map;

public interface MarketSnapshot {
    Map<String, Map<Double,Long>> getSnapshot();
    void saveMarketTick(MarketTick marketTick) throws InterruptedException;
}
