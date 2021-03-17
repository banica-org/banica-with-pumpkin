package com.market.banica.calculator.model;

import com.market.banica.calculator.enums.UnitOfMeasure;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Component
public class Product {

    private String productName;

    private UnitOfMeasure unitOfMeasure;

    private Map<String,Integer> quantityPerParent = new HashMap<>();

    private List<String> ingredients = new ArrayList<>();

    private boolean isDeleted = false;
}
