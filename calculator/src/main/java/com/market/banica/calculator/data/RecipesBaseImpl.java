package com.market.banica.calculator.data;

import com.market.banica.calculator.data.contract.RecipesBase;
import com.market.banica.calculator.model.Product;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Getter
@ToString
@NoArgsConstructor
public class RecipesBaseImpl implements RecipesBase {

    private final Map<String, Product> database = new ConcurrentHashMap<>();
}
