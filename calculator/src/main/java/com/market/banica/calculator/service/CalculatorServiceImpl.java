package com.market.banica.calculator.service;

import com.market.banica.calculator.dto.RecipeDTO;
import com.market.banica.calculator.exception.exceptions.FeatureNotSupportedException;
import com.market.banica.calculator.service.contract.CalculatorService;
import com.market.banica.calculator.service.grpcService.AuroraClientSideService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Date: 3/10/2021 Time: 5:28 PM
 * <p>
 *
 * @author Vladislav_Zlatanov
 */
@Service
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class CalculatorServiceImpl implements CalculatorService {

    AuroraClientSideService auroraService;

    /**
     * @param itemName name of the item (ex:banica)
     * @param quantity quantity of the item
     * @return recipe for item
     */
    @Override
    public RecipeDTO getRecipe(String clientId, String itemName, int quantity) {

        throw new FeatureNotSupportedException("Feature is not implemented yet.");

        //Due the connection is fake atm.
        //method will fail because of the lack of real connection to aurora service.

        //get recipe from property

        //get desired ingredients name

        //call aurora service for specific ingredient price.

        //IngredientResponse ingredients = auroraService.getIngredient(ingredient, quantity);

    }
}
