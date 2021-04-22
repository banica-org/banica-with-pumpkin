package com.market.banica.calculator.service.contract;

public interface JMXServiceMBean {

    void createProduct(String newRecipeName, String unitOfMeasure, String ingredients);

    void addIngredient(String recipeName, String ingredientName, int quantity);

    void setProductQuantity(String recipeName, String ingredientName, int newQuantity);

    long getProductQuantity(String recipeName, String ingredientName);

    String getUnitOfMeasure(String productName);

    void setUnitOfMeasure(String productName, String unitOfMeasure);

    void deleteProductFromDatabase(String ingredientName);

    void deleteProductFromParentIngredients(String parentProductName, String productName);
}
