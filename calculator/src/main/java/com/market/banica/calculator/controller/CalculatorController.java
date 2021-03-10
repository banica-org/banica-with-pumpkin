package com.market.banica.calculator.controller;

import com.market.banica.calculator.dto.RecipeDTO;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

/**
 * Date: 3/10/2021 Time: 7:44 AM
 * <p>
 * TODO: Create service for CalculatorController
 *
 * @author Vladislav_Zlatanov
 */

@RestController
@RequestMapping("/calculator")
@AllArgsConstructor
public class CalculatorController {


    @GetMapping("/{clientId}/{itemName}/{quantity}")
    public ResponseEntity<RecipeDTO> getRecipe(@PathVariable("clientId") @NotBlank String clientId,
                                               @PathVariable("itemName") @NotBlank String itemName,
                                               @PathVariable("quantity") @Min(1) int quantity){

        //Here service should be called
        //replace new RecipeDTO with service call when service is completed
        RecipeDTO recipeDTO = new RecipeDTO();


        return ResponseEntity.ok().body(recipeDTO);
    }
}
