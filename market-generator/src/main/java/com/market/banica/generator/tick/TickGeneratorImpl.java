package com.market.banica.generator.tick;

import com.market.banica.generator.model.GoodSpecification;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

@Getter
public class TickGeneratorImpl implements TickGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(TickGeneratorImpl.class);

    private final Map<String, TickGeneratorTask> tickGeneratorTasks;
    private final String originGood;
    private final BlockingQueue<MarketTick> tickBlockingQueue;

    @Autowired
    public TickGeneratorImpl(String originGood, BlockingQueue<MarketTick> tickBlockingQueue) {
        this.tickGeneratorTasks = new HashMap<>();
        this.originGood = originGood;
        this.tickBlockingQueue = tickBlockingQueue;
    }

    @Override
    public void startTickGeneration(String nameGood, GoodSpecification goodSpecification) {
        if (!tickGeneratorTasks.containsKey(nameGood)) {
            TickGeneratorTask tickGeneratorTask = new TickGeneratorTask(goodSpecification, originGood,
                    nameGood, tickBlockingQueue);
            tickGeneratorTasks.put(nameGood, tickGeneratorTask);
            tickGeneratorTask.run();
            LOGGER.info("Started new tick generation for {}!", nameGood);
        }
    }

    @Override
    public void stopTickGeneration(String nameGood) {
        if (tickGeneratorTasks.containsKey(nameGood)) {
            tickGeneratorTasks.get(nameGood).stop();
            tickGeneratorTasks.remove(nameGood);
            LOGGER.info("Stopped tick generation for {}!", nameGood);
        }
    }

    @Override
    public void updateTickGeneration(String nameGood, GoodSpecification goodSpecification) {
        if (tickGeneratorTasks.containsKey(nameGood)) {
            tickGeneratorTasks.get(nameGood).changeSpecification(goodSpecification);
            LOGGER.info("Updated good specification for {}!", nameGood);
        }
    }
}