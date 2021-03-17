package com.market.banica.generator.tick;

import com.market.banica.generator.model.GoodSpecification;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

@Getter
@Service
public class TickGeneratorImpl implements TickGenerator {

    private Map<String, TickGeneratorTask> tickGeneratorTasks;
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
        }
    }

    @Override
    public void stopTickGeneration(String nameGood) {
        if (tickGeneratorTasks.containsKey(nameGood)) {
            tickGeneratorTasks.get(nameGood).stop();
            tickGeneratorTasks.remove(nameGood);
        }
    }

    @Override
    public void updateTickGeneration(String nameGood, GoodSpecification goodSpecification) {
        if (tickGeneratorTasks.containsKey(nameGood)) {
            tickGeneratorTasks.get(nameGood).changeSpecification(goodSpecification);
        }
    }
}
