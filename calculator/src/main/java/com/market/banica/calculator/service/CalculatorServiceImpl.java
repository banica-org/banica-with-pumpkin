package com.market.banica.calculator.service;

import com.market.banica.calculator.dto.RecipeDTO;
import com.market.banica.calculator.exception.exceptions.FeatureNotSupportedException;
import com.market.banica.calculator.service.contract.CalculatorService;
import org.springframework.stereotype.Service;

/**
 * Date: 3/10/2021 Time: 5:28 PM
 * <p>
 *
 * @author Vladislav_Zlatanov
 */
@Service
public class CalculatorServiceImpl implements CalculatorService {

    /**
     * TODO:Implement method logic
     *
     * @param itemName name of the item (ex:banica)
     * @param quantity quantity of the item
     * @return recipe for item
     */
    @Override
    public RecipeDTO getRecipe(String itemName, double quantity) {

        throw new FeatureNotSupportedException("Feature is not implemented yet.");

    }
}
