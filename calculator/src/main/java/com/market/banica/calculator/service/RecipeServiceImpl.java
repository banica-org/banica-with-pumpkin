package com.market.banica.calculator.service;

import com.market.banica.calculator.model.Recipe;
import com.market.banica.calculator.repository.RecipeRepository;
import com.market.banica.calculator.service.contract.RecipeService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RecipeServiceImpl implements RecipeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeServiceImpl.class);

    private final RecipeRepository recipeRepository;

    @Override
    public Recipe addRecipe(Recipe recipe) {
        LOGGER.debug("Recipe service impl: In addRecipe method");

        Recipe result = recipeRepository.save(recipe);
        LOGGER.info("Database called with save for recipe entity");
        LOGGER.debug("Recipe with id {} successfully added", result.getId());

        return result;
    }

    @Override
    public Recipe updateRecipe(Recipe recipe) {
        LOGGER.debug("Recipe service impl: In updateRecipe method");

        Recipe result = recipeRepository.save(recipe);
        LOGGER.info("Database called with save for recipe entity");
        LOGGER.debug("Recipe with id {} successfully updated", result.getId());

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
    public List<Recipe> getAllRecipes() {
        LOGGER.debug("Recipe service impl: In getAllRecipes method");

        List<Recipe>recipes = getAllRecipesFromDatabase();

        LOGGER.debug("List with recipes with length {} successfully retrieved from database", recipes.size());
        return recipes;
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

        Optional<Recipe> recipe = recipeRepository.findByRecipeName(recipeName);
        LOGGER.info("Database called with save for recipe entity");

        return recipe;
    }

    private List<Recipe> getAllRecipesFromDatabase() {
        LOGGER.debug("Recipe service impl: In getAllRecipesFromDatabase private method");

        List<Recipe> result = recipeRepository.findAll();
        LOGGER.info("Database called with findAll for recipe entity");

        return result;
    }

}
