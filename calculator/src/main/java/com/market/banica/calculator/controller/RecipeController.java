package com.market.banica.calculator.controller;

import com.market.banica.calculator.model.Recipe;
import com.market.banica.calculator.service.contract.RecipeService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "recipe")
public class RecipeController {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeController.class);

    private final RecipeService recipeService;

    @PostMapping
    public ResponseEntity<Recipe> createRecipe( @RequestBody final Recipe recipe) {
        LOGGER.info("POST /recipe called");

        LOGGER.debug("Recipe controller: in createRecipe method");
        return ResponseEntity.ok().body(recipeService.safeRecipe(recipe));
    }
}
