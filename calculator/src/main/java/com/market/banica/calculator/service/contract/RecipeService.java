package com.market.banica.calculator.service.contract;

import com.market.banica.calculator.model.Recipe;

import java.util.List;

public interface RecipeService {

    Recipe addRecipe(Recipe recipe);

    Recipe getRecipe(String recipeName);

    Recipe updateRecipe(Recipe recipe);

    List<Recipe> getAllRecipes();

}
