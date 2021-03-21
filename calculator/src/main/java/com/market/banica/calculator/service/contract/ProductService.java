package com.market.banica.calculator.service.contract;

import com.market.banica.calculator.dto.ProductDto;
import com.market.banica.calculator.model.Product;
import org.springframework.lang.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ProductService {

    Map<String, Product> getProductBase();

    Product createProduct(List<Product> products);

    List<ProductDto> getProductAsListProductDto(String recipeName);

    void getAllProductsAsListProductDto();

    void validateProductsOfListExists(Collection<String> productsNames);

    void validateProductExists(String productName);

    boolean doesProductExists(String productName);

    Product getProductFromDatabase(String productName);

    void addProductToDatabase(String newProductName, Product newProduct);

}
