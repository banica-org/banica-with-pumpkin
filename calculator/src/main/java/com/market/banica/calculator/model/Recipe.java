package com.market.banica.calculator.model;

import lombok.Data;

import java.util.List;

@Data
public class Recipe {

    private String name;

    private List<Ingredient> ingredients;
}
