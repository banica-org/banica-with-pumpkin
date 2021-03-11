package com.market.banica.calculator;

import com.market.banica.calculator.data.ReceiptsBase;
import com.market.banica.calculator.model.Ingredient;
import com.market.banica.calculator.model.Receipt;
import com.market.banica.calculator.service.ReceiptServiceImpl;
import com.market.banica.calculator.service.contract.ReceiptService;
import io.swagger.models.auth.In;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
public class CalculatorApplication {


    public static void main(String[] args) {
        SpringApplication.run(CalculatorApplication.class, args);

        Receipt receipt = new Receipt();
        receipt.setName("banica");
        Ingredient ingredient = new Ingredient();
        ingredient.setName("eggs");
        ingredient.setQuantity("12");
        List<Ingredient> ingredients = new ArrayList<>();
        receipt.setIngredients(ingredients);
        ingredients.add(ingredient);
        List<Receipt>receipts = new ArrayList<>();
        receipts.add(receipt);
        ReceiptsBase receiptsBase = new ReceiptsBase();
        ReceiptService receiptService = new ReceiptServiceImpl(receiptsBase);
        receiptService.createReceipt(receipts);
    }
}
