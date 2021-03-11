package com.market.banica.calculator.controller;

import com.market.banica.calculator.model.Receipt;
import com.market.banica.calculator.service.contract.ReceiptService;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
@RequestMapping(value = "receipt")
public class ReceiptController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReceiptController.class);

    private final ReceiptService receiptService;

    @PostMapping
    public ResponseEntity<Receipt> createReceipt(@Valid @RequestBody final Receipt receipt) {
        LOGGER.info("POST /receipt called");

        LOGGER.debug("Receipt controller: in createReceipt method");
        return ResponseEntity.ok().body(receiptService.createReceipt(receipt));
    }
}
