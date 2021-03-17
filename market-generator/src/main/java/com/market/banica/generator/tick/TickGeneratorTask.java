package com.market.banica.generator.tick;


import com.market.banica.generator.model.GoodSpecification;
import lombok.Getter;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Getter
public class TickGeneratorTask implements Runnable {

    private GoodSpecification goodSpecification;
    private final String originGood;
    private final String nameGood;
    private final BlockingQueue<MarketTick> tickBlockingQueue;
    private final ScheduledExecutorService scheduledExecutorService;
    private volatile static boolean isStopped;

    public TickGeneratorTask(GoodSpecification goodSpecification, String originGood,
                             String nameGood, BlockingQueue<MarketTick> tickBlockingQueue) {
        this.goodSpecification = goodSpecification;
        this.originGood = originGood;
        this.nameGood = nameGood;
        this.tickBlockingQueue = tickBlockingQueue;
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
                e.printStackTrace();
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