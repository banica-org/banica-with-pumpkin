package com.market.banica.generator.service.tickgeneration;

import com.market.banica.generator.model.GoodSpecification;
import com.market.banica.generator.model.MarketTick;
import com.market.banica.generator.service.MarketState;
import com.market.banica.generator.service.MarketStateImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TickGeneratorImpl implements TickGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(TickGeneratorImpl.class);

    private static final Timer TICK_TIMER = new Timer();

    private static final Map<String, TickTimerTask> tickTimerTasks = new ConcurrentHashMap<>();

    private final MarketState marketState;

    @Autowired
    public TickGeneratorImpl(@Value("${market.name}") String marketName, MarketState marketState) {
        MarketTick.setOrigin(marketName);
        this.marketState = marketState;
    }

    @Override
    public void startTickGeneration(GoodSpecification goodSpecification) {
        if (!tickTimerTasks.containsKey(goodSpecification.getName())) {
            TickTimerTask startedTask = new TickTimerTask(this, goodSpecification, marketState);
            tickTimerTasks.put(goodSpecification.getName(), startedTask);
            TICK_TIMER.schedule(startedTask, startedTask.generateRandomPeriod());
            LOGGER.info("Started new tick generation for {}!", goodSpecification.getName());
        } else {
            LOGGER.warn("Could not start new tick generation for {} as it already exists!",
                    goodSpecification.getName());
        }
    }

    @Override
    public void stopTickGeneration(String nameGood) {
        if (tickTimerTasks.containsKey(nameGood)) {
            tickTimerTasks.remove(nameGood).cancel();
            LOGGER.info("Stopped tick generation for {}!", nameGood);
        } else {
            LOGGER.warn("Could not stop tick generation for {} as it does not exist!",
                    nameGood);
        }
    }

    @Override
    public void updateTickGeneration(GoodSpecification goodSpecification) {
        if (tickTimerTasks.containsKey(goodSpecification.getName())) {
            tickTimerTasks.remove(goodSpecification.getName()).cancel();

            TickTimerTask startedTask = new TickTimerTask(this, goodSpecification, marketState);
            tickTimerTasks.put(goodSpecification.getName(), startedTask);
            TICK_TIMER.schedule(startedTask, startedTask.generateRandomPeriod());
            LOGGER.info("Updated tick generation for {}!", goodSpecification.getName());
        } else {
            LOGGER.warn("Could not update tick generation for {} as it does not exist!",
                    goodSpecification.getName());
        }
    }

    public Timer getTickTimer() {
        return TICK_TIMER;
    }

    public Map<String, TickTimerTask> getTickTimerTasks() {
        return tickTimerTasks;
    }
}