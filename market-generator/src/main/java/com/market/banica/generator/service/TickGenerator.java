package com.market.banica.generator.service;

import com.market.TickResponse;

import java.util.List;

public interface TickGenerator {
    List<TickResponse> generateTicks(String topic);
}
