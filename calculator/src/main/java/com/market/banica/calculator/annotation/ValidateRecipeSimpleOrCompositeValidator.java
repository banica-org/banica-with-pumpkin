package com.market.banica.calculator.annotation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyAccessorFactory;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.util.ObjectUtils.isEmpty;

@Slf4j
public class ValidateRecipeSimpleOrCompositeValidator implements ConstraintValidator<ValidateRecipeSimpleOrComposite, Object> {

    private String[] compositeFieldsNames;
    private String[] simpleFieldsNames;
    private String message;

    @Override
    public void initialize(ValidateRecipeSimpleOrComposite constraintAnnotation) {

        compositeFieldsNames = constraintAnnotation.compositeFields();
        simpleFieldsNames = constraintAnnotation.simpleFields();
        message = constraintAnnotation.message();
    }

    @Override
    public boolean isValid(Object objectToValidate, ConstraintValidatorContext context) {
        log.debug("ValidateRecipeSimpleOrCompositeValidator: In isValid method");

        boolean valid;
        try {
            BeanWrapper wrapper = createAccessorForFieldsOfObjectToValidate(objectToValidate);

            List<Object> compositeFieldsValues = extractFieldsValuesAsArrayFromSpringContext(wrapper, compositeFieldsNames);
            List<Object> simpleFieldsValues = extractFieldsValuesAsArrayFromSpringContext(wrapper, simpleFieldsNames);

            if (areAllFieldsNullOrEmpty(compositeFieldsValues)) {

                valid = areOppositeFieldsValid(context, wrapper, simpleFieldsNames);

            } else if (areAllFieldsNullOrEmpty(simpleFieldsValues)) {

                valid = areOppositeFieldsValid(context, wrapper, compositeFieldsNames);

            } else {
                valid = false;

            }
        } catch (BeansException e) {
            log.error("Field or method is not present on class : {}, exception : {}", objectToValidate.getClass().getName(), e);

            return false;
        }

        return valid;
    }

    private BeanWrapper createAccessorForFieldsOfObjectToValidate(Object objectToValidate) {
        log.debug("ValidateRecipeSimpleOrCompositeValidator: In createAccessorForFieldsOfObjectToValidate private method");

        return PropertyAccessorFactory.forBeanPropertyAccess(objectToValidate);
    }

    private List<Object> extractFieldsValuesAsArrayFromSpringContext(BeanWrapper wrapper, String[] selectedFields) {
        log.debug("ValidateRecipeSimpleOrCompositeValidator: In extractFieldsValuesAsArrayFromSpringContext private method");

        List<Object> result = new ArrayList<>();
        for (String selectedField : selectedFields) {
            result.add(wrapper.getPropertyValue(selectedField));
        }
        return result;
    }

    private boolean areOppositeFieldsValid(ConstraintValidatorContext context, BeanWrapper wrapper, String[] oppositeFields) {
        log.debug("ValidateRecipeSimpleOrCompositeValidator: In areOppositeFieldsValid private method");

        boolean valid = true;
        for (String propName : oppositeFields) {

            Object actualValue = extractFieldValueFromSpringContext(wrapper, propName);
            valid = isFieldNotNullOrEmpty(actualValue);

            if (!valid) {
                createConstraintViolationIfNotValid(context, propName);
                break;
            }
        }

        return valid;
    }

    private Object extractFieldValueFromSpringContext(BeanWrapper wrapper, String propName) {
        log.debug("ValidateRecipeSimpleOrCompositeValidator: In extractFieldValueFromSpringContext private method");

        return wrapper.getPropertyValue(propName);
    }

    private boolean isFieldNotNullOrEmpty(Object requiredValue) {
        log.debug("ValidateRecipeSimpleOrCompositeValidator: In areFieldsNotNullOrEmpty private method");

        return requiredValue != null && !isEmpty(requiredValue);
    }

    private void createConstraintViolationIfNotValid(ConstraintValidatorContext context, String propName) {
        log.debug("ValidateRecipeSimpleOrCompositeValidator: In createConstraintViolationIfNotValid private method");

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addPropertyNode(propName).addConstraintViolation();
        log.error("Constraint violated for field {}", propName);
    }

    private boolean areAllFieldsNullOrEmpty(List<Object> selectedFields) {
        log.debug("ValidateRecipeSimpleOrCompositeValidator: In areAllSelectedFieldsNull private method");

        return selectedFields.stream().allMatch(field -> field == null || isEmpty(field));
    }
}

