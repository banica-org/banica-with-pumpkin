package com.market.banica.generator.tick;

import com.market.TickResponse;
import com.market.banica.generator.model.GoodSpecification;

import java.util.List;

public interface TickGenerator {
    void startTickGeneration(String good, GoodSpecification goodSpecification);

    void stopTickGeneration(String good);

    void updateTickGeneration(String good, GoodSpecification goodSpecification);

    List<TickResponse> generateTicks(String goodName);
}