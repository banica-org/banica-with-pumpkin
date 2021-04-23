package com.market.banica.calculator.controller;

import com.market.banica.calculator.dto.ProductDto;
import com.market.banica.common.exception.ProductNotAvailableException;
import com.market.banica.calculator.service.contract.CalculatorService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * Date: 3/10/2021 Time: 7:44 AM
 * <p>
 *
 * @author Vladislav_Zlatanov
 */

@RestController
@RequestMapping(value = "calculator")
@RequiredArgsConstructor
public class CalculatorController {

    private final CalculatorService service;
    private static final Logger LOGGER = LoggerFactory.getLogger(CalculatorController.class);

    @GetMapping("/{clientId}/{itemName}/{quantity}")
    public List<ProductDto> getProduct(@PathVariable("clientId") @NotBlank String clientId,
                                       @PathVariable("itemName") @NotBlank String itemName,
                                       @PathVariable("quantity") @Min(1) long quantity) throws ProductNotAvailableException {
        LOGGER.info("GET /calculator called");


        return service.getProduct(clientId, itemName, quantity);
    }
}
