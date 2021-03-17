package com.market.banica.calculator.data;

import com.market.banica.calculator.data.contract.RecipesBase;
import com.market.banica.calculator.model.Product;
import lombok.Getter;
import lombok.ToString;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Getter
@ToString
public class RecipesBaseImpl implements RecipesBase {

    private final Map<String,Product> database = new ConcurrentHashMap<>();
}
