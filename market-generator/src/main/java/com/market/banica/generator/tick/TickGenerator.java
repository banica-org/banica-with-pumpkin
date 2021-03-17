package com.market.banica.generator.tick;

import com.market.banica.generator.model.GoodSpecification;

public interface TickGenerator {
    public void startTickGeneration(String good, GoodSpecification goodSpecification);

    public void stopTickGeneration(String good);

    public void updateTickGeneration(String good, GoodSpecification goodSpecification);
}
