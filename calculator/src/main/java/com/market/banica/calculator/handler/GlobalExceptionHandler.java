package com.market.banica.calculator.handler;

import com.market.banica.common.exceptions.IncorrectResponseException;
import com.market.banica.common.exceptions.StoppedStreamException;
import com.market.banica.common.exceptions.TrackingException;
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
        LOGGER.error("Exception from type {} with {} message was thrown.",exception.getClass().toString(),exception.getMessage());
        return new ResponseEntity<>(new ErrorMessage(exception.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ErrorMessage> globalExceptionsHandler(Exception exception) {
        LOGGER.error("Exception from type {} with {} message was thrown.",exception.getClass().toString(),exception.getMessage());
        return new ResponseEntity<>(new ErrorMessage(exception.getMessage()), HttpStatus.BAD_REQUEST);
    }
}
