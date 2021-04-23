package com.market.banica.calculator.service.contract;

import com.market.banica.calculator.dto.ProductDto;
import com.market.banica.calculator.exception.exceptions.ProductNotAvailableException;

import java.util.List;

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
    List<ProductDto> getProduct(String clientId, String itemName, long quantity) throws ProductNotAvailableException;
}
