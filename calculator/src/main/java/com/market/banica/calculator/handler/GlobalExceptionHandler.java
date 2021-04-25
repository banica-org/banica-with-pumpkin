package com.market.banica.calculator.handler;

import com.market.banica.common.exception.ProductNotAvailableException;
import com.market.banica.common.exception.IncorrectResponseException;
import com.market.banica.common.exception.TrackingException;
import org.modelmapper.spi.ErrorMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;


//@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {


    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorMessage> illegalArgumentExceptionHandler(IllegalArgumentException exception) {
        return new ResponseEntity<>(new ErrorMessage(exception.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ProductNotAvailableException.class)
    public ResponseEntity<ErrorMessage> productNotAvailableExceptionHandler(ProductNotAvailableException exception) {
        return new ResponseEntity<>(new ErrorMessage(exception.getMessage()), HttpStatus.OK);
    }

    @ExceptionHandler({IncorrectResponseException.class, TrackingException.class,Exception.class})
    public ResponseEntity<ErrorMessage> customExceptionHandler(Exception exception) {
        return new ResponseEntity<>(new ErrorMessage(exception.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
    }


}
