package com.market.banica.calculator.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Data
public class RecipeBase {

    private String itemName;

    private BigDecimal totalPrice;

    private List<ProductSpecification> productSpecifications = new ArrayList<>();

    private  Set<RecipeBase> ingredients;
}
