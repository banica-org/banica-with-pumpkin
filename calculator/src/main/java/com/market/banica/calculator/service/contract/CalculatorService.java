package com.market.banica.calculator.service.contract;

import com.market.banica.calculator.dto.RecipeDTO;

/**
 * Date: 3/10/2021 Time: 5:29 PM
 * <p>
 *
 * @author Vladislav_Zlatanov
 */
public interface CalculatorService {

    /**
     * @param itemName name of the item (ex:banica)
     * @param quantity quantity of the item
     * @return recipe for item
     */
    RecipeDTO getRecipe(String itemName, int quantity);
}
