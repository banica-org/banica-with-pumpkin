package com.market.banica.calculator.service.contract;

import com.market.banica.calculator.model.Product;

import java.util.Collection;
import java.util.List;

public interface ProductService {

    Product createProduct(List<Product> products);

    List<Product> getProductAsListProduct(String recipeName);

    void getAllProductsAsListProduct();

    void validateProductsOfListExists(Collection<String> productsNames);

    void validateProductExists(String productName);

    boolean doesProductExists(String productName);

    Product getProductFromDatabase(String productName);

    void addProductToDatabase(String newProductName, Product newProduct);

}
