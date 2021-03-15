package com.market.banica.calculator.service.contract;

import com.market.banica.calculator.model.Recipe;

import java.util.List;
import java.util.Map;

public interface RecipeService {

    Recipe safeRecipe(Recipe recipe);

    Recipe getRecipe(String recipeName);

    Map<String, Integer> getAllRecipeIngredientsWithQuantities(String recipeName);

    List<Recipe> getAllRecipes();

}
