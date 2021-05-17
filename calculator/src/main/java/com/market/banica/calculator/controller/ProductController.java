package com.market.banica.calculator.controller;

import com.market.banica.calculator.model.Product;
import com.market.banica.calculator.service.contract.ProductService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "product")
public class ProductController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductController.class);

    private final ProductService productService;

    @PostMapping
    public Product createProduct(@RequestBody final List<Product> products) {
        LOGGER.info("POST /product called");
        LOGGER.debug("Product controller: in createProduct method");
        return productService.createProduct(products);
    }
}
