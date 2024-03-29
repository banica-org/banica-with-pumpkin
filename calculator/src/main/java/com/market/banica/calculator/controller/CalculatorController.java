package com.market.banica.calculator.controller;

import com.market.banica.calculator.dto.ItemDto;
import com.market.banica.calculator.dto.ProductDto;
import com.market.banica.calculator.service.contract.CalculatorService;
import com.market.banica.calculator.service.contract.TransactionService;
import com.market.banica.common.exception.ProductNotAvailableException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.util.List;

@RestController
@RequestMapping(value = "calculator")
@RequiredArgsConstructor
public class CalculatorController {

    private static final Logger LOGGER = LoggerFactory.getLogger(CalculatorController.class);
    private final CalculatorService calculatorService;
    private final TransactionService transactionService;

    @GetMapping("/{clientId}/{itemName}/{quantity}")
    public List<ProductDto> getBestPriceForRecipe(@PathVariable("clientId") @NotBlank String clientId,
                                                  @PathVariable("itemName") @NotBlank String itemName,
                                                  @PathVariable("quantity") @Min(1) long quantity) throws ProductNotAvailableException {
        LOGGER.info("GET /calculator called.");
        return calculatorService.getProduct(clientId, itemName, quantity);
    }

    @GetMapping("/buy/{clientId}/{itemName}/{quantity}")
    public List<ProductDto> buyProduct(@PathVariable("clientId") @NotBlank String clientId,
                                       @PathVariable("itemName") @NotBlank String itemName,
                                       @PathVariable("quantity") @Min(1) long quantity) throws ProductNotAvailableException {
        LOGGER.info("GET /calculator/buy/{clientId}/{itemName}/{quantity} method is called.");
        return transactionService.buyProduct(clientId, itemName, quantity);
    }

    @PostMapping("/sell/{clientId}")
    public String sellProduct(@RequestBody final List<ItemDto> products) {
        LOGGER.info("POST /calculator/sell/{clientId} method is called.");
        return transactionService.sellProduct(products);
    }
}
