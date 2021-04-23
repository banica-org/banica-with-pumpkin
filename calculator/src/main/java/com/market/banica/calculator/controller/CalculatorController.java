package com.market.banica.calculator.controller;

import com.market.banica.calculator.dto.ProductDto;
import com.market.banica.calculator.service.contract.CalculatorService;
import com.market.banica.calculator.service.contract.TransactionService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private final TransactionService transactionService;
    private static final Logger LOGGER = LoggerFactory.getLogger(CalculatorController.class);

    @GetMapping("/{clientId}/{itemName}/{quantity}")
    public List<ProductDto> getBestPriceForProductProduct(@PathVariable("clientId") @NotBlank String clientId,
                                                          @PathVariable("itemName") @NotBlank String itemName,
                                                          @PathVariable("quantity") @Min(1) long quantity) {
        LOGGER.info("GET /calculator called");

// az da
        return service.getProduct(clientId, itemName, quantity);
    }

    @GetMapping("/buy/{clientId}/{itemName}/{quantity}")
    public List<ProductDto> buyProduct(@PathVariable("clientId") @NotBlank String clientId,
                                       @PathVariable("itemName") @NotBlank String itemName,
                                       @PathVariable("quantity") @Min(1) long quantity) {
        LOGGER.info("GET /calculator called");

        List<ProductDto> productDtos = transactionService.buyProduct(clientId, itemName, quantity);

        return productDtos;
    }
}
