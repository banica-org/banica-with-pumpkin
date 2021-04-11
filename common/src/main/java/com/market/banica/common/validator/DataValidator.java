package com.market.banica.common.validator;

public class DataValidator {

    public static void checkForValidData(String parameter) {
        if (parameter == null || parameter.isEmpty()) {
            throw new IllegalArgumentException("The incoming data is invalid!");
        }
    }
}
