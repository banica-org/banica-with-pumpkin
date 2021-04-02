package com.market.banica.generator.service;

import com.market.banica.generator.model.GoodSpecification;
import com.market.banica.generator.model.MarketTick;
import com.market.banica.generator.service.task.TickTask;
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

    private static final ScheduledExecutorService TICK_SCHEDULER = Executors.newScheduledThreadPool(5);

    private static final Map<String, ScheduledFuture<?>> TICK_TASKS = new ConcurrentHashMap<>();

    private final MarketState marketState;

    @Autowired
    public TickGeneratorImpl(@Value("${market.name}") String marketName, MarketState marketState) {
        MarketTick.setOrigin(marketName);
        this.marketState = marketState;
    }

    @Override
    public void startTickGeneration(GoodSpecification goodSpecification) {
        if (!TICK_TASKS.containsKey(goodSpecification.getName())) {
            TickTask startedTask = new TickTask(this, goodSpecification);
            ScheduledFuture<?> taskScheduledFuture = TICK_SCHEDULER.schedule(startedTask,
                    startedTask.generateRandomPeriod(), TimeUnit.SECONDS);
            TICK_TASKS.put(goodSpecification.getName(), taskScheduledFuture);

            LOGGER.info("Started new tick generation for {}!", goodSpecification.getName());
        } else {
            LOGGER.warn("Could not start new tick generation for {} as it already exists!",
                    goodSpecification.getName());
        }
    }

    @Override
    public void stopTickGeneration(String nameGood) {
        if (TICK_TASKS.containsKey(nameGood)) {
            TICK_TASKS.remove(nameGood).cancel(true);
            LOGGER.info("Stopped tick generation for {}!", nameGood);
        } else {
            LOGGER.warn("Could not stop tick generation for {} as it does not exist!", nameGood);
        }
    }

    @Override
    public void updateTickGeneration(GoodSpecification goodSpecification) {
        if (TICK_TASKS.containsKey(goodSpecification.getName())) {
            TICK_TASKS.remove(goodSpecification.getName()).cancel(true);

            TickTask startedTask = new TickTask(this, goodSpecification);
            ScheduledFuture<?> taskScheduledFuture = TICK_SCHEDULER.schedule(startedTask,
                    startedTask.generateRandomPeriod(), TimeUnit.SECONDS);
            TICK_TASKS.put(goodSpecification.getName(), taskScheduledFuture);

            LOGGER.info("Updated tick generation for {}!", goodSpecification.getName());
        } else {
            LOGGER.warn("Could not update tick generation for {} as it does not exist!",
                    goodSpecification.getName());
        }
    }

    @Override
    public void executeTickTask(MarketTick marketTick, TickTask nextTick, long delay) {

        marketState.addTickToMarketState(marketTick);

        ScheduledFuture<?> taskScheduledFuture = TICK_SCHEDULER.schedule(nextTick, delay, TimeUnit.SECONDS);

        TICK_TASKS.put(marketTick.getGood(), taskScheduledFuture);

    }

}