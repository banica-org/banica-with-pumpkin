package com.market.banica.calculator.dto;

import com.market.banica.calculator.enums.UnitOfMeasure;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class ProductDto {

    private String productName;

    private UnitOfMeasure unitOfMeasure;

    private Map<String,Integer> quantityPerParent = new HashMap<>();

}
