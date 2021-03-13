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
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "receipt")
public class ReceiptController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReceiptController.class);

    private final RecipeService recipeService;

    @PostMapping
    public ResponseEntity<String> createReceipt(@Valid @RequestBody final List<Recipe> recipes) {
        LOGGER.info("POST /receipt called");

        LOGGER.debug("Receipt controller: in createReceipt method");
        return ResponseEntity.ok().body(recipeService.createRecipe(recipes));
    }
}
