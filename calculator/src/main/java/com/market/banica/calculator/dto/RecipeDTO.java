package com.market.banica.calculator.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Date: 3/10/2021 Time: 7:38 AM
 * <p>
 * Class is representation of an recipe
 *
 * @author Vladislav_Zlatanov
 */

@Data
@NoArgsConstructor
public class RecipeDTO {
    //Name of the recipe
    private String itemName;

    //total price of the recipe
    //price should be evaluated from total price of the ingredients
    private BigDecimal totalPrice;

    //ingredients required for the recipe
    Set<IngredientDTO> ingredients;
}
