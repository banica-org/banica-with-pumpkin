package com.market.banica.generator.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;

@AllArgsConstructor
@Getter
@EqualsAndHashCode
public class GoodSpecification {
    private final String name;
    private final long quantityLow;
    private final long quantityHigh;
    private final long quantityStep;
    private final double priceLow;
    private final double priceHigh;
    private final double priceStep;
    private final int periodLow;
    private final int periodHigh;
    private final int periodStep;

    public Map<String, String> generateProperties(String origin) {
        Map<String, String> pMap = new LinkedHashMap<>();
        pMap.put(createKeyString(origin, "quantityrange", "low"), String.valueOf(getQuantityLow()));
        pMap.put(createKeyString(origin, "quantityrange", "high"), String.valueOf(getQuantityHigh()));
        pMap.put(createKeyString(origin, "quantityrange", "step"), String.valueOf(getQuantityStep()));
        pMap.put(createKeyString(origin, "pricerange", "low"), String.valueOf(getPriceLow()));
        pMap.put(createKeyString(origin, "pricerange", "high"), String.valueOf(getPriceHigh()));
        pMap.put(createKeyString(origin, "pricerange", "step"), String.valueOf(getPriceStep()));
        pMap.put(createKeyString(origin, "tickrange", "low"), String.valueOf(getPeriodLow()));
        pMap.put(createKeyString(origin, "tickrange", "high"), String.valueOf(getPeriodHigh()));
        pMap.put(createKeyString(origin, "tickrange", "step"), String.valueOf(getPeriodStep()));
        return pMap;
    }

    private String createKeyString(String origin, String property, String range) {
        return String.format("%s.%s.%s.%s", origin, this.name, property, range);
    }
}
