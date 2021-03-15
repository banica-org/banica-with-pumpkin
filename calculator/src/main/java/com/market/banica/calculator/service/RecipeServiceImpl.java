package com.market.banica.calculator.service;

import com.market.banica.calculator.model.Recipe;
import com.market.banica.calculator.repository.RecipeRepository;
import com.market.banica.calculator.service.contract.RecipeService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;

@Service
@RequiredArgsConstructor
public class RecipeServiceImpl implements RecipeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeServiceImpl.class);

    private final RecipeRepository recipeRepository;

    @Override
    public Recipe safeRecipe(Recipe recipe) {
        LOGGER.debug("Recipe service impl: In safeRecipe method");

        Recipe result = recipeRepository.save(recipe);
        LOGGER.info("Database called with save for recipe entity");
        LOGGER.debug("Recipe with id {} successfully added", result.getId());

        return result;
    }

    @Override
    public Recipe getRecipe(String recipeName) {
        LOGGER.debug("Recipe service impl: In getRecipe method");

        Optional<Recipe> recipe = getRecipeFromDatabase(recipeName);

        Recipe result = validateRecipeExist(recipeName, recipe);

        LOGGER.debug("Recipe with id {} successfully retrieved from database", result.getId());
        return result;
    }

    @Override
    public Map<String, Integer> getAllRecipeIngredientsWithQuantities(String recipeName) {
        LOGGER.debug("Recipe service impl: In getAllRecipeIngredientsWithQuantities method");

        Recipe recipe = getRecipe(recipeName);

        Map<String, Integer> result = new HashMap<>();

        groupAllIngredientsFromRecipeInResultMap(result,recipe);

        LOGGER.debug("Ingredients with quantities for recipe with id {} successfully retrieved from database",
                recipe.getId());
        return result;
    }

    @Override
    public List<Recipe> getAllRecipes() {
        LOGGER.debug("Recipe service impl: In getAllRecipes method");

        List<Recipe> recipes = getAllRecipesFromDatabase();

        LOGGER.debug("List with recipes with length {} successfully retrieved from database", recipes.size());
        return recipes;
    }

    private void groupAllIngredientsFromRecipeInResultMap(Map<String, Integer> result, Recipe recipe) {
        LOGGER.debug("Recipe service impl: In groupAllIngredientsFromRecipeInResultMap private method");

        Queue<Recipe> tempContainer = new ArrayDeque<>(recipe.getIngredients());

        while (!tempContainer.isEmpty()) {
            Recipe tempRecipe = tempContainer.remove();
            if(tempRecipe.getIngredientName() == null){
                tempContainer.addAll(tempRecipe.getIngredients());
            }else{
                result.merge(tempRecipe.getIngredientName(), tempRecipe.getQuantity(), Integer::sum);
            }
        }
    }

    private Recipe validateRecipeExist(String recipeName, Optional<Recipe> recipe) {
        LOGGER.debug("Recipe service impl: In validateRecipeExist private method");

        if (!recipe.isPresent()) {

            LOGGER.error("Recipe with name: {} not found. Exception thrown", recipeName);
            throw new IllegalArgumentException("Recipe not found");
        }
        return recipe.get();
    }

    private Optional<Recipe> getRecipeFromDatabase(String recipeName) {
        LOGGER.debug("Recipe service impl: In getRecipeFromDatabase private method");

        Optional<Recipe> recipe = recipeRepository.findByRecipeNameAndIsDeletedFalse(recipeName);
        LOGGER.info("Database called with save for recipe entity");

        return recipe;
    }

    private List<Recipe> getAllRecipesFromDatabase() {
        LOGGER.debug("Recipe service impl: In getAllRecipesFromDatabase private method");

        List<Recipe> result = recipeRepository.findAllByIsDeletedFalse();
        LOGGER.info("Database called with findAll for recipe entity");

        return result;
    }

}
