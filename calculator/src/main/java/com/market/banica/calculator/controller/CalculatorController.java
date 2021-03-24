package com.market.banica.calculator.controller;

import com.market.banica.calculator.dto.RecipeDTO;
import com.market.banica.calculator.service.contract.CalculatorService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

/**
 * Date: 3/10/2021 Time: 7:44 AM
 * <p>
 *
 * @author Vladislav_Zlatanov
 */

@RestController
@RequestMapping("/calculator")
@AllArgsConstructor
public class CalculatorController {

    @Autowired
    CalculatorService service;

    //TODO: Change in master return type
    @GetMapping("/{clientId}/{itemName}/{quantity}")
    public RecipeDTO getRecipe(@PathVariable("clientId") @NotBlank String clientId,
                               @PathVariable("itemName") @NotBlank String itemName,
                               @PathVariable("quantity") @Min(1) int quantity) {

        //Here service should be called
        //replace new RecipeDTO with service call when service is completed


        return service.getRecipe(clientId, itemName, quantity);
    }
}
