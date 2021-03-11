package com.market.banica.calculator.service;

import com.market.banica.calculator.model.Ingredient;
import com.market.banica.calculator.model.Receipt;
import com.market.banica.calculator.service.contract.ReceiptService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

@Service
public class ReceiptServiceImpl implements ReceiptService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReceiptServiceImpl.class);

    @Override
    public Receipt createReceipt(Receipt receipt) {
        LOGGER.debug("Receipt service impl: In createReceipt method");

        Properties props = new Properties();

        try (OutputStream output =
                     new FileOutputStream("src/main/resources/" + receipt.getName() + ".properties")) {

            for (Ingredient ingredient : receipt.getIngredients()) {
                props.setProperty(ingredient.getName(), ingredient.getQuantity());
            }

            props.store(output, receipt.getName() + " receipt:");
            LOGGER.debug("Receipt {} successfully created",receipt);

        } catch (IOException ex) {
            LOGGER.error("Unable to create receipt {}", receipt);
        }

        return receipt;
    }

}
