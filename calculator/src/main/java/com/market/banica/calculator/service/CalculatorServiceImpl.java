package com.market.banica.calculator.service;

import com.market.banica.calculator.dto.ProductSpecification;
import com.market.banica.calculator.dto.RecipeBase;
import com.market.banica.calculator.dto.RecipeDTO;
import com.market.banica.calculator.model.Product;
import com.market.banica.calculator.service.contract.CalculatorService;
import com.market.banica.calculator.service.contract.ProductService;
import com.market.banica.calculator.service.grpc.AuroraClientSideService;
import com.orderbook.ItemOrderBookResponse;
import com.orderbook.OrderBookLayer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class CalculatorServiceImpl implements CalculatorService {


    private final AuroraClientSideService auroraService;
    private final ProductService productService;

    @Override
    public RecipeDTO getRecipe(String clientId, String itemName, int quantity) {

        List<Product> products = productService.getProductAsListProduct(itemName);

        Map<String, List<RecipeBase>> productAvailability = new HashMap<>();
        List<RecipeBase> recipeBaseList = new ArrayList<>();
        for (Product product : products) {
            ItemOrderBookResponse itemOrderBookResponse = auroraService.getIngredient(product.getProductName(), clientId);

            RecipeBase recipeBase = new RecipeBase();
            String productName = itemOrderBookResponse.getItemName();
            recipeBase.setItemName(productName);

            List<ProductSpecification> productSpecifications = new ArrayList<>();

            for (OrderBookLayer orderBookLayer : itemOrderBookResponse.getOrderbookLayersList()) {

                ProductSpecification productSpecification = new ProductSpecification();

                productSpecification.setPrice(BigDecimal.valueOf(orderBookLayer.getPrice()));
                productSpecification.setLocation(orderBookLayer.getOrigin().toString());
                productSpecification.setQuantity(orderBookLayer.getQuantity());

                productSpecifications.add(productSpecification);
            }
            recipeBase.setProductSpecifications(productSpecifications);

            if(!product.getIngredients().isEmpty()){

                Queue<Product> tempContainer = product.getIngredients().keySet().stream()
                        .map(k->products.stream().filter(l->l.getProductName().equals(k)).findFirst().orElseThrow(()->new IllegalArgumentException()))
                        .collect(Collectors.toCollection(ArrayDeque::new));
            }


            recipeBaseList.add(recipeBase);
            productAvailability.put(itemName, recipeBaseList);
        }


        return new RecipeDTO();
    }
}
