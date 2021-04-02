package com.market.banica.generator.service.task;

import com.market.banica.generator.model.GoodSpecification;
import com.market.banica.generator.model.MarketTick;
import com.market.banica.generator.service.TickGenerator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.Random;

public class TickTask implements Runnable {

    private static final Random RANDOM = new Random();

    private final TickGenerator tickGenerator;
    private final GoodSpecification good;

    public TickTask(TickGenerator tickGenerator, GoodSpecification good) {
        this.tickGenerator = tickGenerator;
        this.good = good;
    }

    @Override
    public void run() {

        MarketTick currentTick = new MarketTick(good.getName(),
                generateRandomQuantity(),
                generateRandomPrice(),
                new Date().getTime());

        TickTask nextTick = new TickTask(tickGenerator, good);
        tickGenerator.executeTickTask(currentTick, nextTick, generateRandomPeriod());

    }

    public int generateRandomPeriod() {
        int randomInRange = Math.abs(RANDOM.nextInt() % (good.getPeriodHigh() + 1 - good.getPeriodLow()));
        return randomInRange - (good.getPeriodStep() > 0 ? randomInRange % good.getPeriodStep() : 0)
                + good.getPeriodLow();
    }

    private long generateRandomQuantity() {
        long randomInRange = Math.abs(RANDOM.nextLong() % (good.getQuantityHigh() + 1 - good.getQuantityLow()));
        return randomInRange - (good.getQuantityStep() > 0 ? randomInRange % good.getQuantityStep() : 0)
                + good.getQuantityLow();
    }

    private double generateRandomPrice() {
        double randomInRange = RANDOM.nextDouble() * (good.getPriceHigh() + good.getPriceStep() - good.getPriceLow());
        return roundingPrice(randomInRange - (good.getPriceStep() > 0 ? randomInRange % good.getPriceStep() : 0)
                + good.getPriceLow());
    }

    private static double roundingPrice(double price) {
        return BigDecimal.valueOf(price)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

}
