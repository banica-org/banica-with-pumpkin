package com.market.banica.calculator.service.contract;

import com.market.banica.calculator.model.Pair;
import com.market.banica.calculator.model.Product;

import java.util.List;
import java.util.Map;

public interface ProductService {

    Product createProduct(List<Product> products);

    void createProduct(String newProductName, String unitOfMeasure,
                       String ingredientsMap);

    void addIngredient(String parentProductName, String productName, long quantity);

    void setProductQuantity(String parentProductName, String productName, long newQuantity);

    long getProductQuantity(String parentProductName, String productName);

    String getUnitOfMeasure(String productName);

    void setUnitOfMeasure(String productName, String unitOfMeasure);

    void deleteProductFromDatabase(String productName);

    void deleteProductFromParentIngredients(String parentProductName, String productName);

    Map<Product, Map<String, Pair<Long, Long>>> getProductIngredientsWithQuantityPerParent(String productName, long orderedQuantity);

    Product getProductFromDatabase(String productName);
}
