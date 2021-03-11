package com.market.banica.calculator.configuration;

import com.market.banica.calculator.data.ReceiptsBase;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.util.StringUtils;

import java.util.Arrays;

@Configuration
@EnableMBeanExport
@ManagedResource
@RequiredArgsConstructor
public class JMXConfig {

    private final ReceiptsBase receiptsBase;

    @ManagedOperation
    public void setValue(String receiptName, String ingredientName, String newValue) {

        String receipt = receiptsBase.getDatabase().getProperty(receiptName);
        String[] ingredients = receipt.split(",");

        for (int i = 0; i < ingredients.length; i++) {
           if(ingredients[i].startsWith(ingredientName)){
               ingredients[i] = ingredientName + ":" + newValue;
           }
        }

        String newReceipt = StringUtils.collectionToCommaDelimitedString(Arrays.asList(ingredients));
        receiptsBase.getDatabase().setProperty(receiptName,newReceipt);
    }

    @ManagedOperation
    public String getValue(String receiptName, String ingredientName) {

        String receipt = receiptsBase.getDatabase().getProperty(receiptName);
        String[] ingredients = receipt.split(",");

        for (String ingredient : ingredients) {
            if (ingredient.startsWith(ingredientName)) {
                return ingredient;
            }
        }
        return "Ingredient not found";
    }


}
