package com.market.banica.calculator.service.contract;

import com.market.banica.calculator.model.Product;

import java.util.List;

public interface ProductService {

    Product createProduct(List<Product> products);

    void createProduct(String newProductName, String unitOfMeasure,
                          String ingredientsMap);

    void addIngredient(String parentProductName, String productName, int quantity);

    void setProductQuantity(String parentProductName, String productName, int newQuantity);

    int getProductQuantity(String parentProductName, String productName);

    String getUnitOfMeasure(String productName);

    void setUnitOfMeasure(String productName, String unitOfMeasure);

    void deleteProductFromDatabase(String productName);

    void deleteProductFromParentIngredients(String parentProductName, String productName);

    List<Product> getProductAsListProduct(String recipeName);

    void getAllProductsAsListProduct();
}
