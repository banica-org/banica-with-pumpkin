package com.market.banica.generator.service;

import com.market.TickResponse;
import com.market.banica.generator.model.GoodSpecification;
import com.market.banica.generator.model.MarketTick;
import com.market.banica.generator.service.task.TickTimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

@Component
public class TickGeneratorImpl implements TickGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(TickGeneratorImpl.class);

    private static final Timer TICK_TIMER = new Timer();
    private static final BlockingQueue<MarketTick> generatedTicks = new LinkedBlockingQueue<>();
    private static final Map<String, TickTimerTask> tickTimerTasks = new ConcurrentHashMap<>();

    @Autowired
    public TickGeneratorImpl(@Value("${market.name}") String marketName) {
        MarketTick.setOrigin(marketName);
    }

    @Override
    public void startTickGeneration(GoodSpecification goodSpecification) {
        if (!tickTimerTasks.containsKey(goodSpecification.getName())) {
            TickTimerTask tickTimerTask = new TickTimerTask(this, goodSpecification);
            tickTimerTasks.put(goodSpecification.getName(), tickTimerTask);
            TICK_TIMER.schedule(tickTimerTask, tickTimerTask.generateRandomPeriod());
            LOGGER.info("Started new tick generation for {}!", goodSpecification.getName());
        } else {
            LOGGER.warn("Could not start new tick generation for {} as it already exists!",
                    goodSpecification.getName());
        }
    }

    @Override
    public void stopTickGeneration(String nameGood) {
        if (tickTimerTasks.containsKey(nameGood)) {
            tickTimerTasks.get(nameGood).cancel();
            LOGGER.info("Stopped tick generation for {}!", nameGood);
        } else {
            LOGGER.warn("Could not stop tick generation for {} as it does not exist!",
                    nameGood);
        }
    }

    @Override
    public void updateTickGeneration(GoodSpecification goodSpecification) {
        if (tickTimerTasks.containsKey(goodSpecification.getName())) {
            TickTimerTask tickTimerTask = tickTimerTasks.get(goodSpecification.getName());
            tickTimerTask.cancel();
            tickTimerTask.changeSpecification(goodSpecification);
            TICK_TIMER.schedule(tickTimerTask, tickTimerTask.generateRandomPeriod());
            LOGGER.info("Updated good specification for {}!", goodSpecification.getName());
        } else {
            LOGGER.warn("Could not update tick generation for {} as it does not exist!",
                    goodSpecification.getName());
        }
    }

    @Override
    public List<TickResponse> getGeneratedTicksForGood(String nameGood) {
        return generatedTicks.stream()
                .filter(marketTick -> marketTick.getGood().equals(nameGood))
                .map(marketTick -> TickResponse.newBuilder()
                        .setOrigin(MarketTick.getOrigin())
                        .setGoodName(marketTick.getGood())
                        .setQuantity(marketTick.getQuantity())
                        .setPrice(marketTick.getPrice())
                        .setTimestamp(marketTick.getDate().getTime())
                        .build())
                .collect(Collectors.toList());
    }

    public Timer getTickTimer() {
        return TICK_TIMER;
    }

    public BlockingQueue<MarketTick> getGeneratedTicks() {
        return generatedTicks;
    }

    public Map<String, TickTimerTask> getTickTimerTasks() {
        return tickTimerTasks;
    }

}