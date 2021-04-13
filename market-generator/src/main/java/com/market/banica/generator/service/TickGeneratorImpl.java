package com.market.banica.generator.service;

import com.market.banica.generator.model.GoodSpecification;
import com.market.banica.generator.model.MarketTick;
import com.market.banica.generator.task.TickTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Component
public class TickGeneratorImpl implements TickGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(TickGeneratorImpl.class);

    private final ScheduledExecutorService tickScheduler = Executors.newScheduledThreadPool(5);

    private final Map<String, ScheduledFuture<?>> tickTasks = new ConcurrentHashMap<>();

    private final MarketState marketState;

    @Autowired
    public TickGeneratorImpl(@Value("${market.name}") String marketName, MarketState marketState) {
        MarketTick.setOrigin(marketName);
        this.marketState = marketState;
    }

    @Override
    public void startTickGeneration(GoodSpecification goodSpecification) {

        String nameGood = goodSpecification.getName();

        if (!tickTasks.containsKey(nameGood)) {
            TickTask startedTask = new TickTask(this, goodSpecification);
            ScheduledFuture<?> taskScheduledFuture = tickScheduler.schedule(startedTask,
                    startedTask.generateRandomPeriod(), TimeUnit.SECONDS);
            tickTasks.put(nameGood, taskScheduledFuture);

            LOGGER.info("Started new tick generation for {}!", nameGood);
        } else {
            LOGGER.warn("Could not start new tick generation for {} as it already exists!", nameGood);
        }

    }

    @Override
    public void stopTickGeneration(String nameGood) {

        if (tickTasks.containsKey(nameGood)) {
            tickTasks.remove(nameGood).cancel(true);
            LOGGER.info("Stopped tick generation for {}!", nameGood);
        } else {
            LOGGER.warn("Could not stop tick generation for {} as it does not exist!", nameGood);
        }

    }

    @Override
    public void updateTickGeneration(GoodSpecification goodSpecification) {

        String nameGood = goodSpecification.getName();

        if (tickTasks.containsKey(nameGood)) {
            tickTasks.remove(nameGood).cancel(true);

            TickTask startedTask = new TickTask(this, goodSpecification);
            ScheduledFuture<?> taskScheduledFuture = tickScheduler.schedule(startedTask,
                    startedTask.generateRandomPeriod(), TimeUnit.SECONDS);
            tickTasks.put(nameGood, taskScheduledFuture);

            LOGGER.info("Updated tick generation for {}!", nameGood);
        } else {
            LOGGER.warn("Could not update tick generation for {} as it does not exist!", nameGood);
        }

    }

    @Override
    public void executeTickTask(MarketTick marketTick, TickTask nextTick, long delay) {

        marketState.addTickToMarketState(marketTick);

        ScheduledFuture<?> taskScheduledFuture = tickScheduler.schedule(nextTick, delay, TimeUnit.SECONDS);

        tickTasks.put(marketTick.getGood(), taskScheduledFuture);

    }

}