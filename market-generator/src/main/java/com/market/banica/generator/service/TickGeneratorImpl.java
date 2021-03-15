package com.market.banica.generator.service;

import com.market.TickResponse;

import java.util.Arrays;
import java.util.List;

public class TickGeneratorImpl implements TickGenerator {

    @Override
    public List<TickResponse> generateTicks(String topic) {
        return Arrays.asList(TickResponse.newBuilder().setItemName("tick1").build(),
                             TickResponse.newBuilder().setItemName("tick2").build());
    }
}
