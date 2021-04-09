package com.market.banica.generator.service;

import com.market.TickResponse;
import com.market.banica.generator.model.MarketTick;
import com.market.banica.generator.util.PersistScheduler;

import java.util.List;

public interface MarketState {

    void addTickToMarketState(MarketTick marketTick);

    List<TickResponse> generateMarketTicks(String good);

    PersistScheduler getPersistScheduler();

}