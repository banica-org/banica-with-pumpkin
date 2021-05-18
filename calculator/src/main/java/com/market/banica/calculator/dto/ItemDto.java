package com.market.banica.calculator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class ItemDto {
    private String name;
    @DecimalMin(value = "0.1", message = "Item's price should be grater than 0,0.")
    private BigDecimal price;
    private String location;
    @Min(value = 1, message = "Item's quantity should be grater than 0.")
    private Long quantity;
}
