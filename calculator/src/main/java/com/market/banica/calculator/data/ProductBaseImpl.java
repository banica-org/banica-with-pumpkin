package com.market.banica.calculator.data;

import com.market.banica.calculator.data.contract.ProductBase;
import com.market.banica.calculator.model.Product;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Getter
@ToString
@NoArgsConstructor
public class ProductBaseImpl implements ProductBase {

    private final Map<String, Product> database = new ConcurrentHashMap<>();
}
