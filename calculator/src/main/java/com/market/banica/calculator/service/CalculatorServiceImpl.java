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
import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class CalculatorServiceImpl implements CalculatorService {


    private final AuroraClientSideService auroraService;
    private final ProductService productService;
    public static final Integer QUANTITY = 10;

    @Override
    public RecipeDTO getRecipe(String clientId, String itemName, int quantity) {

        List<Product> products = productService.getProductAsListProduct(itemName);

        Map<String, List<RecipeBase>> productAvailability = new HashMap<>();
        List<RecipeBase> recipeBaseList = new ArrayList<>();
        for (Product product : products) {
            ItemOrderBookResponse itemOrderBookResponse = auroraService.getIngredient(product.getProductName(), clientId, QUANTITY);

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

            if (!product.getIngredients().isEmpty()) {

                Queue<Product> tempContainer = product.getIngredients().keySet().stream()
                        .map(k -> products.stream().filter(l -> l.getProductName().equals(k)).findFirst().orElseThrow(() -> new IllegalArgumentException()))
                        .collect(Collectors.toCollection(ArrayDeque::new));
            }


            recipeBaseList.add(recipeBase);
            productAvailability.put(itemName, recipeBaseList);
        }


        return new RecipeDTO();
    }

    public RecipeDTO getBestPriceForRecipe(String clientId, String itemName, int quantity) {

        List<Product> products = productService.getProductAsListProduct(itemName);

        Map<String, List<ProductSpecification>> productSpecificationMap = new HashMap<>();

        Product parentProduct = products.stream().filter(product -> product.getProductName().equals(itemName)).findFirst().orElseThrow(IllegalArgumentException::new);

        generateProductSpecificationData(clientId, products, productSpecificationMap, parentProduct);


        return new RecipeDTO();
    }

    private void generateProductSpecificationData(String clientId, List<Product> products, Map<String, List<ProductSpecification>> productSpecificationMap, Product product) {

        fillProductSpecificationMapWithData(clientId, productSpecificationMap, product);

        if (!product.getIngredients().isEmpty()) {

            product.getIngredients()
                    .keySet()
                    .stream()
                    .map(ingredientName -> products
                            .stream()
                            .filter(prod -> prod.getProductName().equals(ingredientName))
                            .findFirst()
                            .orElseThrow(IllegalArgumentException::new))
                    .forEach(ingredient -> generateProductSpecificationData(clientId, products, productSpecificationMap, ingredient));
                  /*  .collect(Collectors.toList())
                    .forEach(ingredient -> generateProductSpecificationData(clientId, products, productSpecificationMap, product));*/


        }
    }

    private void fillProductSpecificationMapWithData(String clientId, Map<String, List<ProductSpecification>> productSpecificationMap, Product product) {
        ItemOrderBookResponse orderBookResponse = auroraService.getIngredient(product.getProductName(), clientId, QUANTITY);

        String productName = orderBookResponse.getItemName();

        productSpecificationMap.put(productName, new ArrayList<>());

        for (OrderBookLayer layer : orderBookResponse.getOrderbookLayersList()) {
            ProductSpecification productSpecification = createProductSpecification(layer);
            productSpecificationMap.get(productName).add(productSpecification);
        }
    }

    private ProductSpecification createProductSpecification(OrderBookLayer layer) {
        ProductSpecification productSpecification = new ProductSpecification();
        productSpecification.setPrice(BigDecimal.valueOf(layer.getPrice()));
        productSpecification.setQuantity(layer.getQuantity());
        productSpecification.setLocation(layer.getOrigin().toString());
        return productSpecification;
    }
}
