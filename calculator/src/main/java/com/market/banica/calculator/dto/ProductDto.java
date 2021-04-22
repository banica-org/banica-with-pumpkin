package com.market.banica.calculator.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Data
public class ProductDto {

    private String itemName;
    private BigDecimal totalPrice = BigDecimal.ZERO;
    private List<ProductSpecification> productSpecifications = new ArrayList<>();
    private Map<String, Long> ingredients = new HashMap<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProductDto)) {
            return false;
        }
        ProductDto that = (ProductDto) o;
        return Objects.equals(getItemName(), that.getItemName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getItemName());
    }
}
