package com.market.banica.generator.tick;

import com.market.Origin;
import com.market.TickResponse;
import com.market.banica.generator.model.GoodSpecification;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;

@Getter
@Component
public class TickGeneratorImpl implements TickGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(TickGeneratorImpl.class);

    private final Map<String, TickGeneratorTask> tickGeneratorTasks;

    @Autowired
    public TickGeneratorImpl() {
        this.tickGeneratorTasks = new HashMap<>();
    }

    @Override
    public void startTickGeneration(String nameGood, GoodSpecification goodSpecification) {
        if (!tickGeneratorTasks.containsKey(nameGood)) {
            TickGeneratorTask tickGeneratorTask = new TickGeneratorTask(goodSpecification,
                    nameGood.split("/")[0],
                    nameGood);
            tickGeneratorTasks.put(nameGood, tickGeneratorTask);
            tickGeneratorTasks.get(nameGood).run();
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

    @Override
    public List<TickResponse> generateTicks(String nameGood) {
        if (!tickGeneratorTasks.containsKey(nameGood)) {
            return new ArrayList<>();
        }
        BlockingQueue<MarketTick> queueTicks = tickGeneratorTasks.get(nameGood).getTickBlockingQueue();
        List<TickResponse> listTicks = new ArrayList<>();
        while (!queueTicks.isEmpty()) {
            Date date = new Date();
            MarketTick marketTick = queueTicks.remove();
            //removes all ticks?
            TickResponse tickResponse = TickResponse.newBuilder()
                    .setOrigin(Origin.valueOf(marketTick.getOrigin().toUpperCase()))
                    .setPrice(marketTick.getPrice())
                    .setQuantity(marketTick.getAmount())
                    .setGoodName(marketTick.getGood())
                    .setTimestamp(date.getTime())
                    .build();
            listTicks.add(tickResponse);
        }
        return listTicks;
    }

    public List<String> getMarketCatalogue(String marketName) {
        return this.tickGeneratorTasks.keySet().stream()
                .filter(k -> k.split("/")[0].equals(marketName))
                .map(k -> k.split("/")[1])
                .collect(Collectors.toList());
    }
}