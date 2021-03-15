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
public class JMXComponent {

    private static final Logger LOGGER = LoggerFactory.getLogger(JMXComponent.class);

    private final RecipeService recipeService;

    @ManagedOperation
    public void deleteIngredient(String recipeName, String ingredientName){
        long start = System.currentTimeMillis();

        LOGGER.debug("JMXConfig: In deleteIngredient method");
        LOGGER.info("DeleteIngredient called from JMX server with parameters recipeName {},ingredientName {}",
                recipeName, ingredientName);

        Recipe recipe = retrieveRecipeFromDatabase(recipeName);

        String bottomIngredientName = ingredientName;
        if (isNestedIngredient(ingredientName)) {

            String[] ingredients = getNestedIngredientsAsStringArray(ingredientName);
            bottomIngredientName = getLastProductFromChain(ingredients);

            recipe = getBottomRecipe(recipe, ingredients);
        }

        validateBottomIngredientExist(recipe, bottomIngredientName);

        deleteIngredientInRecipe(bottomIngredientName,recipe);
        LOGGER.debug("Ingredient deleted from JMX server for recipe {} and ingredient {}"
                , recipeName, ingredientName);

        long stop = System.currentTimeMillis();
        System.out.println(stop - start + "deleteIngredient");
    }

    @ManagedOperation
    public void addIngredient(String recipeName, String ingredientName, String quantityAsString){
        long start = System.currentTimeMillis();

        LOGGER.debug("JMXConfig: In addIngredient method");
        LOGGER.info("AddIngredient called from JMX server with parameters recipeName {},ingredientName {} and newValue{}",
                recipeName, ingredientName, quantityAsString);

        Recipe recipe = retrieveRecipeFromDatabase(recipeName);
        int quantity = getValueAsInt(quantityAsString);

        String newIngredientName = ingredientName;
        if (isNestedIngredient(ingredientName)) {

            String[] ingredients = getNestedIngredientsAsStringArray(ingredientName);
            newIngredientName = getLastProductFromChain(ingredients);

            recipe = getBottomRecipe(recipe, ingredients);
        }

        addIngredientToRecipe(newIngredientName,recipe,quantity);
        LOGGER.debug("Ingredient added from JMX server for recipe {} and ingredient {} with value {}"
                , recipeName, ingredientName, quantityAsString);

        long stop = System.currentTimeMillis();
        System.out.println(stop - start + " addIngredient");
    }

    @ManagedOperation
    public void setIngredientQuantity(String recipeName, String ingredientName, String newValue) {
        long start = System.currentTimeMillis();

        LOGGER.debug("JMXConfig: In setIngredientQuantity method");
        LOGGER.info("SetIngredientQuantity called from JMX server with parameters recipeName {},ingredientName {} and newValue{}",
                recipeName, ingredientName, newValue);

        Recipe recipe = retrieveRecipeFromDatabase(recipeName);

        int newQuantity = getValueAsInt(newValue);

        String bottomIngredientName = ingredientName;
        if (isNestedIngredient(ingredientName)) {

            String[] ingredients = getNestedIngredientsAsStringArray(ingredientName);
            bottomIngredientName = getLastProductFromChain(ingredients);

            recipe = getBottomRecipe(recipe, ingredients);
        }

        validateBottomIngredientExist(recipe, bottomIngredientName);

        setIngredientQuantityInRecipe(bottomIngredientName,recipe,newQuantity);
        LOGGER.debug("Value set from JMX server for recipe {} and ingredient {} with value {}"
                , recipeName, ingredientName, newValue);

        long stop = System.currentTimeMillis();
        System.out.println(stop - start + " setIngredientQuantity");
    }

    @ManagedOperation
    public String getIngredientQuantity(String recipeName, String ingredientName) {
        long start = System.currentTimeMillis();

        LOGGER.debug("JMXConfig: In getIngredientQuantity method");
        LOGGER.info("GetIngredientQuantity called from JMX server for recipe {} and ingredient {}", recipeName, ingredientName);

        Recipe recipe = retrieveRecipeFromDatabase(recipeName);

        String ingredientQuantity;

        String bottomIngredientName = ingredientName;
        if (isNestedIngredient(ingredientName)) {

            String[] ingredients = getNestedIngredientsAsStringArray(ingredientName);

            bottomIngredientName = getLastProductFromChain(ingredients);

            recipe = getBottomRecipe(recipe, ingredients);
        }

        ingredientQuantity = getIngredientQuantityFromRecipe(bottomIngredientName, recipe);

        LOGGER.debug("Quantity checked from JMX server for recipe {} and ingredient {}. The value is {}",
                recipeName, ingredientName, ingredientQuantity);
        long stop = System.currentTimeMillis();
        System.out.println(stop - start + " getIngredientQuantity");
        return ingredientQuantity;
    }

    private void deleteIngredientInRecipe(String ingredientName, Recipe recipe) {
        LOGGER.debug("JMXConfig: In setIngredientQuantityInRecipe private method");

        for(Recipe ingredient: recipe.getIngredients()){
            if(ingredient.getIngredientName().equals(ingredientName)){
                ingredient.setDeleted(true);
                break;
            }
        }

        recipeService.safeRecipe(recipe);
    }

    private void addIngredientToRecipe(String ingredientName, Recipe recipe, int quantity) {
        LOGGER.debug("JMXConfig: In addIngredientToRecipe private method");

        Recipe newIngredient = createRecipeObject(ingredientName, quantity);

        recipe.getIngredients().add(newIngredient);

        recipeService.safeRecipe(recipe);
    }

    private Recipe createRecipeObject(String ingredientName, int quantity) {
        Recipe newIngredient = new Recipe();
        newIngredient.setIngredientName(ingredientName);
        newIngredient.setQuantity(quantity);
        return newIngredient;
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

    private int getValueAsInt(String newValue) {
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

    private String getLastProductFromChain(String[] ingredients) {
        LOGGER.debug("JMXConfig: In getLastIngredientFromChain private method");

        return ingredients[ingredients.length-1];
    }

    private Recipe retrieveRecipeFromDatabase(String recipeName) {
        LOGGER.debug("JMXConfig: In retrieveRecipeFromDatabase private method");

        return recipeService.getRecipe(recipeName);
    }

    private Recipe getBottomRecipe(Recipe recipe, String[] ingredients) {
        LOGGER.debug("JMXConfig: In getBottomRecipe private method");

        for (int i = 0; i < ingredients.length-1; i++) {

            recipe = getNextRecipe(recipe, ingredients[i]);

        }
        return recipe;
    }

    private String[] getNestedIngredientsAsStringArray(String ingredientName) {
        LOGGER.debug("JMXConfig: In getNestedIngredientsAsStringArray private method");

        return ingredientName.split("/");
    }

    private Recipe getNextRecipe(Recipe recipe, String ingredient) {
        LOGGER.debug("JMXConfig: In getNextRecipe private method");

        recipe = recipe.getIngredients().stream()
                .filter(product->product.getRecipeName() != null)
                .filter(product -> product.getRecipeName().equals(ingredient))
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
                .filter(product->product.getIngredientName() != null)
                .filter(ingredient -> ingredient.getIngredientName().equals(ingredientName))
                .findAny()
                .map(Recipe::getQuantity)
                .map(String::valueOf)
                .orElse("Ingredient not found");

    }

    private void setIngredientQuantityInRecipe(String ingredientName, Recipe recipe, int newQuantity) {
        LOGGER.debug("JMXConfig: In setIngredientQuantityInRecipe private method");
        System.out.println(ingredientName + " " + recipe.toString() +" " + newQuantity);
        for(Recipe ingredient: recipe.getIngredients()){
            if(ingredient.getIngredientName().equals(ingredientName)){
                ingredient.setQuantity(newQuantity);
                break;
            }
        }

        recipeService.safeRecipe(recipe);
    }

    private boolean isNestedIngredient(String ingredientName) {
        LOGGER.debug("JMXConfig: In isNestedIngredient private method");
        return ingredientName.contains("/");
    }
}
