package com.market.banica.calculator.controller;

import com.market.banica.calculator.configuration.BanicaPumpkinProps;
import com.market.banica.calculator.service.contract.BanicaPumpkinPropsConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping(value = "receipt")
public class ReceiptController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReceiptController.class);

    private final BanicaPumpkinPropsConfig banicaPumpkinPropsConfig;

@PostMapping
public ResponseEntity<BanicaPumpkinProps> createReceipt(@Valid @RequestBody final BanicaPumpkinProps banicaPumpkinProps){
    LOGGER.info("POST /receipt called");

    LOGGER.debug("Receipt controller: in createReceipt method");
    return ResponseEntity.ok().body(banicaPumpkinPropsConfig.createReceipt(banicaPumpkinProps));
}
}
