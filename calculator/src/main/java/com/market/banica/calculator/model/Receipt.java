package com.market.banica.calculator.model;

import lombok.Data;

import java.util.List;

@Data
public class Receipt {

    private String name;

    private List<Ingredient> ingredients;
}
