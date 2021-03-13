package com.market.banica.calculator.service;

import com.market.banica.calculator.data.RecipesBase;
import com.market.banica.calculator.model.Ingredient;
import com.market.banica.calculator.model.Recipe;
import com.market.banica.calculator.service.contract.RecipeService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.StringJoiner;

@Service
@RequiredArgsConstructor
public class RecipeServiceImpl implements RecipeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeServiceImpl.class);

    private final RecipesBase recipesBase;

    @Override
    public String createRecipe(List<Recipe> recipes) {
        LOGGER.debug("Recipe service impl: In createRecipe method");

        validateParameterForNullAndEmpty(recipes);

        String recipeName = getRecipeName(recipes);

        validateRecipesFromListForNullAndEmpty(recipes);

        String recipe = getRecipeFromList(recipes);

        writeRecipeToRecipeBase(recipeName, recipe);
        LOGGER.debug("Recipe {} successfully created", recipeName);

        return recipeName + "-" + recipe;
    }

    private void writeRecipeToRecipeBase(String recipeName, String recipe) {
        LOGGER.debug("Recipe service impl: In writeRecipeToRecipeBase private method");

        recipesBase.setPropertyWithBackUp(recipeName, recipe);
    }

    private String getRecipeName(List<Recipe> recipes) {
        LOGGER.debug("Recipe service impl: In getRecipeName private method");

        return recipes.get(0).getName();
    }

    private void validateParameterForNullAndEmpty(List<Recipe> recipes) {
        LOGGER.debug("Recipe service impl: In validateParameterForNullOrEmpty private method");

        if (recipes == null || recipes.size() == 0) {

            LOGGER.error("Parameter {} passed to createRecipe is null or empty",recipes);
            throw new IllegalArgumentException("Recipes should be present to create recipe");
        }
    }

    private void validateRecipesFromListForNullAndEmpty(List<Recipe> recipes){
        LOGGER.debug("Recipe service impl: In validateRecipeListForNullAndEmpty private method");

        for (Recipe recipe : recipes) {

            validateIngredientsForNullOrEmpty(recipe);
        }
    }

    private String getRecipeFromList(List<Recipe> recipes) {
        LOGGER.debug("Recipe service impl: In convertRecipesToString private method");

        StringJoiner stringJoiner = new StringJoiner(",");
        for (Recipe recipe : recipes) {

            convertRecipeToStringBuilder(stringJoiner, recipe);
        }
        return stringJoiner.toString();
    }

    private void convertRecipeToStringBuilder(StringJoiner stringJoiner, Recipe recipe) {
        LOGGER.debug("Recipe service impl: In convertListOfRecipesToStringBuilder private method");

        String delimiterRecipeIngredient = ".";
        String delimiterIngredientQuantity = ":";

        for (Ingredient ingredient : recipe.getIngredients()) {
            stringJoiner.add(recipe.getName() + delimiterRecipeIngredient + ingredient.getName()
                    + delimiterIngredientQuantity + ingredient.getQuantity());
        }
    }

    private void validateIngredientsForNullOrEmpty(Recipe recipe) {
        LOGGER.debug("Recipe service impl: In validateIngredientsForNullOrEmpty private method");

        if (recipe.getIngredients() == null || recipe.getIngredients().size() == 0) {

            LOGGER.error("Parameter {} passed to convertRecipesToString has null or empty fields", recipe);
            throw new IllegalArgumentException("Ingredients should be present to create recipe");
        }
    }

}
