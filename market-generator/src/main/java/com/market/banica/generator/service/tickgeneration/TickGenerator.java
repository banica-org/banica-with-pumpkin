package com.market.banica.generator.service.tickgeneration;

import com.market.banica.generator.model.GoodSpecification;

import java.util.Map;
import java.util.Timer;

public interface TickGenerator {

    Timer getTickTimer();

    Map<String, TickTimerTask> getTickTimerTasks();

    void startTickGeneration(GoodSpecification goodSpecification);

    void stopTickGeneration(String good);

    void updateTickGeneration(GoodSpecification goodSpecification);
}
