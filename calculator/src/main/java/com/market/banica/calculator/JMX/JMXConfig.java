package com.market.banica.calculator.JMX;

import com.market.banica.calculator.model.Recipe;
import com.market.banica.calculator.service.contract.RecipeService;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

@Component
@EnableMBeanExport
@ManagedResource
@ToString
@RequiredArgsConstructor
public class JMXConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(JMXConfig.class);

    private final RecipeService recipeService;

    @ManagedOperation
    public void setValue(String recipeName, String ingredientName, String newValue) {
        LOGGER.debug("JMXConfig: In setValue method");
        LOGGER.info("SetValue called from JMX server with parameters recipeName {},ingredientName {} and newValue{}",
                recipeName, ingredientName, newValue);

        Recipe recipe = retrieveRecipeFromDatabase(recipeName);

        int newQuantity = getNewValueAsInt(newValue);

        String bottomIngredientName = ingredientName;
        if (isNestedIngredient(ingredientName)) {

            String[] ingredients = getNestedIngredientsAsStringArray(ingredientName);
            bottomIngredientName = getLastIngredientFromChain(ingredients);

            recipe = getBottomRecipe(recipe, ingredients);
        }

        validateBottomIngredientExist(recipe, bottomIngredientName);

        setIngredientQuantityInRecipe(ingredientName,recipe,newQuantity);
        LOGGER.debug("Value set from JMX server for recipe {} and ingredient {} with value {}"
                , recipeName, ingredientName, newValue);
    }

    @ManagedOperation
    public String getValue(String recipeName, String ingredientName) {
        LOGGER.debug("JMXConfig: In getValue method");
        LOGGER.info("GetValue called from JMX server for recipe {} and ingredient {}", recipeName, ingredientName);

        Recipe recipe = retrieveRecipeFromDatabase(recipeName);

        String ingredientQuantity;

        String bottomIngredientName = ingredientName;
        if (isNestedIngredient(ingredientName)) {

            String[] ingredients = getNestedIngredientsAsStringArray(ingredientName);
            bottomIngredientName = getLastIngredientFromChain(ingredients);

            recipe = getBottomRecipe(recipe, ingredients);
        }

        ingredientQuantity = getIngredientQuantityFromRecipe(bottomIngredientName, recipe);

        LOGGER.debug("Quantity checked from JMX server for recipe {} and ingredient {}. The value is {}",
                recipeName, ingredientName, ingredientQuantity);
        return ingredientQuantity;
    }

    private void validateBottomIngredientExist(Recipe recipe, String bottomIngredientName) {
        LOGGER.debug("JMXConfig: In validateBottomIngredientExist private method");

        recipe.getIngredients().stream()
                .filter(ingredient -> ingredient.getIngredientName().equals(bottomIngredientName))
                .findFirst()
                .orElseThrow(() -> {
                    LOGGER.error("Ingredient {} not found. Exception thrown", bottomIngredientName);
                    throw new IllegalArgumentException("Ingredient not found");
                });
    }

    private int getNewValueAsInt(String newValue) {
        LOGGER.debug("JMXConfig: In getNewValueAsInt private method");

        try {
            return Integer.parseInt(newValue);
        }catch (NumberFormatException e){
            LOGGER.error("New value passed to setValue is not convertible to int. Exception thrown");
        }catch(NullPointerException e){
            LOGGER.error("New value passed to setValue is null. Exception thrown");
        }
        throw new IllegalArgumentException("Can not convert the new value to number");
    }

    private String getLastIngredientFromChain(String[] ingredients) {
        LOGGER.debug("JMXConfig: In getLastIngredientFromChain private method");

        return ingredients[ingredients.length-1];
    }

    private Recipe retrieveRecipeFromDatabase(String recipeName) {
        LOGGER.debug("JMXConfig: In retrieveRecipeFromDatabase private method");

        return recipeService.getRecipe(recipeName);
    }

    private Recipe getBottomRecipe(Recipe recipe, String[] ingredients) {
        LOGGER.debug("JMXConfig: In getBottomRecipe private method");

        for (int i = 1; i < ingredients.length; i++) {
            recipe = getNextRecipe(recipe, ingredients[i]);
        }
        return recipe;
    }

    private String[] getNestedIngredientsAsStringArray(String ingredientName) {
        LOGGER.debug("JMXConfig: In getNestedIngredientsAsStringArray private method");

        return ingredientName.split(".");
    }

    private Recipe getNextRecipe(Recipe recipe, String ingredient) {
        LOGGER.debug("JMXConfig: In getNextRecipe private method");

        recipe = recipe.getIngredients().stream()
                .filter(product -> product.getIngredientName().equals(ingredient))
                .findFirst()
                .orElseThrow(() -> {
                    LOGGER.error("Ingredient {} not found. Exception thrown", ingredient);
                    throw new IllegalArgumentException("Ingredient not found");
                });
        return recipe;
    }

    private String getIngredientQuantityFromRecipe(String ingredientName, Recipe recipe) {
        LOGGER.debug("JMXConfig: In getIngredientQuantityFromRecipe private method");

        return  recipe.getIngredients().stream()
                .filter(ingredient -> ingredient.getIngredientName().equals(ingredientName))
                .map(Recipe::getQuantity)
                .map(String::valueOf)
                .findFirst()
                .orElse("Ingredient not found");

    }

    private void setIngredientQuantityInRecipe(String ingredientName, Recipe recipe, int newQuantity) {
        LOGGER.debug("JMXConfig: In setIngredientQuantityInRecipe private method");

        for(Recipe ingredient: recipe.getIngredients()){
            if(ingredient.getIngredientName().equals(ingredientName)){
                ingredient.setQuantity(newQuantity);
                break;
            }
        }

        recipeService.updateRecipe(recipe);
    }

    private boolean isNestedIngredient(String ingredientName) {
        LOGGER.debug("JMXConfig: In isNestedIngredient private method");

        return ingredientName.contains(".");
    }
}
