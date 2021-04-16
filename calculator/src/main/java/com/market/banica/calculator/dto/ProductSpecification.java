package com.market.banica.calculator.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductSpecification {

    private BigDecimal price;

    private String location;

    private Long quantity;

}
