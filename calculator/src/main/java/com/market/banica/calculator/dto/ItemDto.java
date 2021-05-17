package com.market.banica.calculator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class ItemDto {
    private String name;
    private BigDecimal price;
    private String location;
    private Long quantity;
}
