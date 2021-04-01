package com.market.banica.calculator.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class ProductDto {

    private String itemName;

    private BigDecimal totalPrice;

    private List<ProductSpecification> productSpecifications = new ArrayList<>();

    private  List<ProductDto> ingredients =  new ArrayList<>();
}
