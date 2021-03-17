package com.market.banica.calculator.service.contract;

import com.market.banica.calculator.model.Product;

import java.util.List;
import java.util.Map;

public interface JMXService {

    Map<String, Product> getDatabase();

    void createProduct(String newRecipeName, String unitOfMeasure, Map<String,Integer> quantityPerParent, List<String> ingredients);

    void addIngredient(String recipeName, String ingredientName, String quantityAsString);

    void setProductQuantity(String recipeName, String ingredientName, String newValue);

    String getProductQuantity(String recipeName, String ingredientName);

    String getUnitOfMeasure(String productName);

    void setUnitOfMeasure(String productName, String unitOfMeasure);

    void deleteRecipe(String recipeName);

    void deleteIngredient(String recipeName, String ingredientName);
}
