package com.market.banica.calculator.service;

import com.market.banica.calculator.data.ReceiptsBase;
import com.market.banica.calculator.model.Ingredient;
import com.market.banica.calculator.model.Receipt;
import com.market.banica.calculator.service.contract.ReceiptService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Properties;

@Service
@RequiredArgsConstructor
public class ReceiptServiceImpl implements ReceiptService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReceiptServiceImpl.class);

    private final ReceiptsBase receiptsBase;

    @Override
    public String createReceipt(List<Receipt> receipts) {
        LOGGER.debug("Receipt service impl: In createReceipt method");

        String receiptName = receipts.get(0).getName();
        StringBuilder stringBuilder = new StringBuilder();

        for (Receipt receipt : receipts) {
            for (Ingredient ingredient : receipt.getIngredients()) {
               stringBuilder.append(receipt.getName()).append(".").append(ingredient.getName())
                       .append(":").append(ingredient.getQuantity());
            }
            stringBuilder.append(",");
        }

        receiptsBase.getDatabase().setProperty(receiptName, stringBuilder.toString());
        LOGGER.debug("Receipt {} successfully created", receiptName);


        return receiptName + "-" + stringBuilder.toString();
    }

}
