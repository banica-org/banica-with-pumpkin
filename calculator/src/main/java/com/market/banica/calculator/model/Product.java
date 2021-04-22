package com.market.banica.calculator.model;

import com.market.banica.calculator.enums.UnitOfMeasure;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Data
@Component
public class Product {

    private String productName;
    private UnitOfMeasure unitOfMeasure;
    private Map<String, Long> ingredients = new HashMap<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Product)) {
            return false;
        }
        Product product = (Product) o;
        return Objects.equals(getProductName(), product.getProductName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getProductName());
    }
}
