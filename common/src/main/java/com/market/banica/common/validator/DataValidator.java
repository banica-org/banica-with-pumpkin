package com.market.banica.common.validator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataValidator {
    public static final String REGEX_INGREDIENTS_PATTERN = "(.+[a-z])(:)(.+\\d)";

    public static void validateIncomingData(String data) {
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("The incoming data must be not null or empty");
        }
    }

    public static void validateIngredientsMap(String ingredientsMap) {

        Pattern pattern = Pattern.compile(REGEX_INGREDIENTS_PATTERN);

        Matcher matcher = pattern.matcher(ingredientsMap);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Ingredients map must follow product:quantity pattern.");
        }

    }
}
