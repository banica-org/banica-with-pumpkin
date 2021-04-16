package com.market.banica.calculator.dto;

import com.market.banica.calculator.model.Pair;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Data
@NoArgsConstructor
public class ProductPriceComponentsSet implements Comparable<ProductPriceComponentsSet> {

    private static final AtomicInteger count = new AtomicInteger(0);

    @EqualsAndHashCode.Exclude
    private final int productId = count.incrementAndGet();

    private String productName;

    private BigDecimal price;

    private Pair<Long, Long> reservedQuantityRangeStartEnd = new Pair<>(0L, 0L);

    private Map<String, List<Integer>> componentIngredients = new HashMap<>();

    @Override
    public int compareTo(ProductPriceComponentsSet o) {

        int result = getPrice().compareTo(o.getPrice());

        if (result == 0) {

            result = o.getProductName().compareTo(getProductName());

            if (result == 0) {

                result = (o.getReservedQuantityRangeStartEnd().hashCode() - getReservedQuantityRangeStartEnd().hashCode());
            }
        }

        return result;
    }
}
