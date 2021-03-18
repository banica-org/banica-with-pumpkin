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
        Map<String, String> propertiesMap = new LinkedHashMap<>();
        propertiesMap.put(createKeyString(origin, "quantityrange", "low"), String.valueOf(getQuantityLow()));
        propertiesMap.put(createKeyString(origin, "quantityrange", "high"), String.valueOf(getQuantityHigh()));
        propertiesMap.put(createKeyString(origin, "quantityrange", "step"), String.valueOf(getQuantityStep()));
        propertiesMap.put(createKeyString(origin, "pricerange", "low"), String.valueOf(getPriceLow()));
        propertiesMap.put(createKeyString(origin, "pricerange", "high"), String.valueOf(getPriceHigh()));
        propertiesMap.put(createKeyString(origin, "pricerange", "step"), String.valueOf(getPriceStep()));
        propertiesMap.put(createKeyString(origin, "tickrange", "low"), String.valueOf(getPeriodLow()));
        propertiesMap.put(createKeyString(origin, "tickrange", "high"), String.valueOf(getPeriodHigh()));
        propertiesMap.put(createKeyString(origin, "tickrange", "step"), String.valueOf(getPeriodStep()));
        return propertiesMap;
    }

    private String createKeyString(String origin, String property, String range) {
        return String.format("%s.%s.%s.%s", origin, this.name, property, range);
    }
}
