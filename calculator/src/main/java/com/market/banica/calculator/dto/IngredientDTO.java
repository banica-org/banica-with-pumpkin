package com.market.banica.calculator.dto;

import com.market.Origin;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;


/**
 * Date: 3/10/2021 Time: 7:31 AM
 * <p>
 * <p>
 * Class is representation of an ingredient
 * in a recipe
 *
 * @author Vladislav_Zlatanov
 */

@Data
@NoArgsConstructor
public class IngredientDTO {

    //Name of the ingredient
    private String itemName;

    //price of the ingredient
    private BigDecimal price;

    //quantity of the ingredient
    private int quantity;

    //location from where the ingredient comes
    private Origin location;
}
