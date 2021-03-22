package com.market.banica.calculator.model;

import com.market.banica.calculator.enums.UnitOfMeasure;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Data
@Component
public class Product  {

    private String productName;

    private UnitOfMeasure unitOfMeasure;

    private Map<String,Integer> ingredients = new HashMap<>();
}
