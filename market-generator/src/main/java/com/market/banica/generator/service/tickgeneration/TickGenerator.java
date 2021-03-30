package com.market.banica.generator.service.tickgeneration;

import com.market.TickResponse;
import com.market.banica.generator.model.GoodSpecification;

import java.util.List;

public interface TickGenerator {

    void startTickGeneration(GoodSpecification goodSpecification);

    void stopTickGeneration(String good);

    void updateTickGeneration(GoodSpecification goodSpecification);

    List<TickResponse> generateTicks(String goodName);
}
