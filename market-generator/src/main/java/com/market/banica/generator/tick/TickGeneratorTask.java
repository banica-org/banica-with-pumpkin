package com.market.banica.generator.tick;


import com.market.banica.generator.model.GoodSpecification;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Getter
public class TickGeneratorTask implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(TickGeneratorTask.class);

    private GoodSpecification goodSpecification;
    private final String originGood;
    private final String nameGood;
    private final BlockingQueue<MarketTick> tickBlockingQueue;
    private final ScheduledExecutorService scheduledExecutorService;
    private volatile boolean isStopped;

    public TickGeneratorTask(GoodSpecification goodSpecification, String originGood,
                             String nameGood) {
        this.goodSpecification = goodSpecification;
        this.originGood = originGood;
        this.nameGood = nameGood;
        this.tickBlockingQueue = new LinkedBlockingQueue<>();
        this.scheduledExecutorService = Executors.newScheduledThreadPool(1);
        isStopped = false;
    }


    public void run() {
        if (!isStopped) {

            double priceGood = generateRandom(goodSpecification.getPriceLow(),
                    goodSpecification.getPriceHigh(), goodSpecification.getPriceStep());
            long amountGood = (long) generateRandom(goodSpecification.getQuantityLow(),
                    goodSpecification.getQuantityHigh(), goodSpecification.getQuantityStep());
            long periodGood = (long) generateRandom((long) goodSpecification.getPeriodLow(),
                    (long) goodSpecification.getPeriodHigh(), (long) goodSpecification.getPeriodStep());

            MarketTick marketTick = new MarketTick(originGood, nameGood,
                    amountGood, priceGood);

            try {
                tickBlockingQueue.put(marketTick);
                scheduledExecutorService.schedule(this, periodGood, TimeUnit.SECONDS);

            } catch (InterruptedException e) {
                LOGGER.error("An error occurred while starting tick generation for {}!", nameGood);
            }
        }
    }

    public synchronized void stop() {
        isStopped = true;
        Thread.currentThread().interrupt();
    }

    public synchronized void changeSpecification(GoodSpecification goodSpecification) {
        this.goodSpecification = goodSpecification;
    }

    private double generateRandom(double lower, double upper, double step) {
        double random = Math.random() * (upper - lower + 1);
        return random - random % step + lower;
    }

}