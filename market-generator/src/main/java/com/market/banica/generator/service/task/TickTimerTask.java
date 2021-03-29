package com.market.banica.generator.service.task;

import com.market.banica.generator.model.GoodSpecification;
import com.market.banica.generator.model.MarketTick;
import com.market.banica.generator.service.TickGenerator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class TickTimerTask extends TimerTask {

    private static final Random RANDOM = new Random();

    private final TickGenerator tickGenerator;
    private final Timer timer;
    private final BlockingQueue<MarketTick> generatedTicks;
    private final Map<String, TickTimerTask> tickTimerTasks;

    private GoodSpecification good;

    public TickTimerTask(TickGenerator tickGenerator, GoodSpecification good) {
        this.tickGenerator = tickGenerator;
        this.timer = tickGenerator.getTickTimer();
        this.generatedTicks = tickGenerator.getGeneratedTicks();
        this.tickTimerTasks = tickGenerator.getTickTimerTasks();
        this.good = good;
    }

    @Override
    public void run() {

        MarketTick marketTick = new MarketTick(good.getName(),
                generateRandomQuantity(),
                generateRandomPrice(),
                new Date());

        generatedTicks.add(marketTick);
        System.out.println(marketTick);

        TickTimerTask nextTick = new TickTimerTask(tickGenerator, good);
        tickTimerTasks.put(good.getName(), nextTick);
        timer.schedule(nextTick, TimeUnit.SECONDS.toMillis(generateRandomPeriod()));

    }

    public int generateRandomPeriod() {
        int randomInRange = RANDOM.nextInt() % (good.getPeriodHigh() + 1 - good.getPeriodLow());
        return randomInRange - (randomInRange % good.getPeriodStep()) + good.getPeriodLow();
    }

    public void changeSpecification(GoodSpecification goodSpecification) {
        this.good = goodSpecification;
    }

    private long generateRandomQuantity() {
        long randomInRange = RANDOM.nextLong() % (good.getQuantityHigh() + 1 - good.getQuantityLow());
        return Math.abs(randomInRange - (randomInRange % good.getQuantityStep()) + good.getQuantityLow());
    }

    private double generateRandomPrice() {
        double randomInRange = RANDOM.nextDouble() * (good.getPriceHigh() + good.getPriceStep() - good.getPriceLow());
        return round(randomInRange - (randomInRange % good.getPriceStep()) + good.getPriceLow());
    }

    private static double round(double value) {
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

}
