package com.market.banica.calculator.exception;

import com.market.banica.calculator.exception.exceptions.IncorrectResponseException;
import com.market.banica.order.book.exception.StoppedStreamException;
import com.market.banica.order.book.exception.TrackingException;
import org.modelmapper.spi.ErrorMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;


@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({IncorrectResponseException.class, StoppedStreamException.class, TrackingException.class})
    public ResponseEntity<ErrorMessage> customExceptionsHandler(Exception exception) {
        return new ResponseEntity<>(new ErrorMessage(exception.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ErrorMessage> globalExceptionsHandler(Exception exception) {
        return new ResponseEntity<>(new ErrorMessage(exception.getMessage()), HttpStatus.BAD_REQUEST);
    }
}
