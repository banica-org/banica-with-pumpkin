package com.market.banica.calculator.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

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

    private BigDecimal totalPrice;

}
