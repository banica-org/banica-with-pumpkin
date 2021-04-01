package com.market.banica.calculator.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class ProductDto {

    private String itemName;

    private BigDecimal totalPrice;

    private List<ProductSpecification> productSpecifications = new ArrayList<>();

    private Map<String,Long> ingredients = new HashMap<>();
}
