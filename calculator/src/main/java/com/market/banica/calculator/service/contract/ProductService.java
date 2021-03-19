package com.market.banica.calculator.service.contract;

import com.market.banica.calculator.dto.ProductDto;
import com.market.banica.calculator.model.Product;
import org.springframework.lang.Nullable;

import java.util.List;

public interface ProductService {

    Product createProduct(List<Product> products);

    List<ProductDto> getProduct(String recipeName, @Nullable String parentRecipeName);

    void getAllProducts();

    void createBackUp();

}
