package com.market.banica.calculator.service;

import com.market.banica.calculator.dto.RecipeDTO;
import com.market.banica.calculator.model.Product;
import com.market.banica.calculator.service.contract.CalculatorService;
import com.market.banica.calculator.service.contract.ProductService;
import com.market.banica.calculator.service.grpc.AuroraClientSideService;
import com.orderbook.ItemOrderBookResponse;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Date: 3/10/2021 Time: 5:28 PM
 * <p>
 *
 * @author Vladislav_Zlatanov
 */
@Service
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class CalculatorServiceImpl implements CalculatorService {

    AuroraClientSideService auroraService;
    ProductService productService;

    /**
     * @param itemName name of the item (ex:banica)
     * @param quantity quantity of the item
     * @return recipe for item
     */
    @Override
    public RecipeDTO getRecipe(String clientId, String itemName, int quantity) {

        List<Product> products = productService.getProductAsListProduct(itemName);
        List<ItemOrderBookResponse> resultList = new ArrayList<>();

        for (int i = 0; i < products.size(); i++) {

            resultList.add(auroraService.getIngredient(itemName, clientId));

        }

        ItemOrderBookResponse product = resultList.get(0);

        RecipeDTO result = new RecipeDTO();
        result.setItemName(product.getItemName());
        result.setIngredients(null);
        result.setTotalPrice(BigDecimal.valueOf(product.getOrderbookLayersList().get(0).getPrice()));

        return result;
    }
}
