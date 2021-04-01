package com.market.banica.generator.service;

import com.market.banica.generator.model.GoodSpecification;
import com.market.banica.generator.service.task.TickTimerTask;

import java.util.Map;
import java.util.Timer;

public interface TickGenerator {

    Timer getTickTimer();

    Map<String, TickTimerTask> getTickTimerTasks();

    void startTickGeneration(GoodSpecification goodSpecification);

    void stopTickGeneration(String good);

    void updateTickGeneration(GoodSpecification goodSpecification);
}
