package com.market.banica.generator.service;

import com.market.banica.generator.model.GoodSpecification;
import com.market.banica.generator.model.MarketTick;
import com.market.banica.generator.task.TickTask;

public interface TickGenerator {

    void startTickGeneration(GoodSpecification goodSpecification);

    void stopTickGeneration(String good);

    void updateTickGeneration(GoodSpecification goodSpecification);

    void executeTickTask(MarketTick marketTick, TickTask nextTick, long delay);

}
