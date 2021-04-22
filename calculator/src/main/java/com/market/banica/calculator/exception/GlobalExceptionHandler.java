package com.market.banica.calculator.exception;

import com.market.banica.calculator.exception.exceptions.BadResponseException;
import com.market.banica.calculator.exception.exceptions.ProductNotAvailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Date: 3/10/2021 Time: 5:30 PM
 * <p>
 * Global exception handler for calculator module
 *
 * @author Vladislav_Zlatanov
 */

@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    protected ResponseEntity<Object> handleIllegalArgument(RuntimeException runtimeException, WebRequest request) {
        return handleExceptionInternal(runtimeException, runtimeException.getMessage(),
                new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler({BadResponseException.class})
    public ResponseEntity<Object> badAuroraResponse(RuntimeException runtimeException, WebRequest request) {
        return handleExceptionInternal(runtimeException, runtimeException.getMessage(),
                new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @ExceptionHandler({ProductNotAvailableException.class})
    public ResponseEntity<Object> handleNotEnoughResources(Exception exception, WebRequest request) {
        return handleExceptionInternal(exception, exception.getMessage(),
                new HttpHeaders(), HttpStatus.OK, request);
    }
}
