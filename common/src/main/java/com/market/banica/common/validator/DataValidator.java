package com.market.banica.common.validator;

import org.springframework.stereotype.Component;

@Component
public class DataValidator {

    public void checkForValidData(String parameter) {
        if (parameter == null || parameter.isEmpty()) {
            throw new IllegalArgumentException("The incoming data is invalid!");
        }
    }
}
