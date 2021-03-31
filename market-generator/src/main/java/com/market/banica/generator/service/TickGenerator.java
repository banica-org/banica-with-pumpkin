package com.market.banica.generator.service;

import com.market.TickResponse;
import com.market.banica.generator.model.GoodSpecification;
import com.market.banica.generator.model.MarketTick;
import com.market.banica.generator.service.task.TickTimerTask;

import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.BlockingQueue;

public interface TickGenerator {

    Timer getTickTimer();

    BlockingQueue<MarketTick> getGeneratedTicks();

    Map<String, TickTimerTask> getTickTimerTasks();

    void startTickGeneration(GoodSpecification goodSpecification);

    void stopTickGeneration(String good);

    void updateTickGeneration(GoodSpecification goodSpecification);

    List<TickResponse> getGeneratedTicksForGood(String goodName);

}
