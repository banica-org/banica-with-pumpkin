package com.market.banica.common.validator;


public class DataValidator {
    public static final String INGREDIENT_NAME_PATTERN = "^[a-zA-Z]*$";
    public static final String INGREDIENT_QUANTITY_PATTERN = "^\\d*\\.?\\d*$";

    public static void validateIncomingData(String data) {
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("The incoming data must be not null or empty");
        }
    }

    public static void validateIngredientsMap(String ingredientsMap) {

        for (String ingredientMap : ingredientsMap.split(",")) {
            String[] ingredientProperties = ingredientMap.split(":");
            if (ingredientProperties.length != 2) {
                throw new IllegalArgumentException("Ingredients map must follow product:quantity pattern");
            }
            String ingredientName = ingredientProperties[0];
            String ingredientQuantity = ingredientProperties[1];
            checkIngredientName(ingredientName);
            checkIngredientQuantity(ingredientQuantity);
        }

    }

    public static void validateItemPriceAndQuantity(String itemName, String marketName, double price, long quantity) {
        if (price <= 0.0) {
            throw new IllegalArgumentException(String.format("Sorry, you can't sell %s to %s market because price should be grater than 0,0.", itemName, marketName));
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException(String.format("Sorry, you can't sell %s to %s market because quantity should be grater than 0.", itemName, marketName));
        }
    }

    private static void checkIngredientName(String ingredientName) {
        if (!ingredientName.matches(INGREDIENT_NAME_PATTERN)) {
            throw new IllegalArgumentException("Ingredient name must contains only letters.");
        }
    }

    private static void checkIngredientQuantity(String ingredientQuantity) {
        if (!ingredientQuantity.matches(INGREDIENT_QUANTITY_PATTERN)) {
            throw new IllegalArgumentException("Ingredient quantity must contains only digits.");
        }
    }
}
