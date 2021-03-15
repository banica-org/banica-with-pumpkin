package com.market.banica.calculator.annotation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Repeatable(ValidateRecipeSimpleOrCompositeValues.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {ValidateRecipeSimpleOrCompositeValidator.class})
public @interface ValidateRecipeSimpleOrComposite {

    String message() default "This field is required.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    String[] compositeFields();
    String[] simpleFields();
}
