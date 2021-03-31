package com.market.banica.calculator.service;

import com.market.banica.calculator.dto.RecipeDTO;
import com.market.banica.calculator.model.Product;
import com.market.banica.calculator.service.contract.CalculatorService;
import com.market.banica.calculator.service.contract.ProductService;
import com.market.banica.calculator.service.grpc.AuroraClientSideService;
import com.orderbook.ItemOrderBookResponse;
import com.orderbook.OrderBookLayer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
@RequiredArgsConstructor
public class CalculatorServiceImpl implements CalculatorService {


    private final AuroraClientSideService auroraService;
    private final ProductService productService;

    @Override
    public RecipeDTO getRecipe(String clientId, String itemName, int quantity) {

        List<Product> products = productService.getProductAsListProduct(itemName);

        while (quantity > 0){
            Map<String, List<OrderBookLayer>> productAvailability = getProductWithAppropriateIngredients(products, clientId);
        }


        return new RecipeDTO();
    }

    private Map<String, List<OrderBookLayer>> getProductWithAppropriateIngredients(List<Product> products, String clientId) {
        Map<String, List<OrderBookLayer>> productAvailability = new HashMap<>();

        for (Product product : products) {
            ItemOrderBookResponse itemOrderBookResponse = auroraService.getIngredient(product.getProductName(), clientId);

            String productName = itemOrderBookResponse.getItemName();
            List<OrderBookLayer> productLayers = itemOrderBookResponse.getOrderbookLayersList();
            productAvailability.put(productName, productLayers);
        }
        return productAvailability;
    }
}
