package com.market.banica.common.validator;

public class DataValidator {

    public static void validateIncomingData(String data) {
        if (data == null || data.isEmpty()){
            throw new IllegalArgumentException("The incoming data must be not null or empty");
        }
    }
}
