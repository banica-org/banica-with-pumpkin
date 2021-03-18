package com.market.banica.calculator.exception;

import com.market.banica.calculator.exception.exceptions.BadResponseException;
import com.market.banica.calculator.exception.exceptions.FeatureNotSupportedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Collections;

/**
 * Date: 3/10/2021 Time: 5:30 PM
 * <p>
 * Global exception handler for calculator module
 *
 * @author Vladislav_Zlatanov
 */

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({FeatureNotSupportedException.class})
    public ResponseEntity<Object> notSupportedFeatureHandling(FeatureNotSupportedException exception) {
        return new ResponseEntity<>(Collections.singletonMap("error", exception.getMessage()), HttpStatus.valueOf(501));
    }

    @ExceptionHandler({BadResponseException.class})
    public void badAuroraResponse(BadResponseException exception){
        LOGGER.warn("Exception caught: {}" ,exception.getMessage());
    }
}
