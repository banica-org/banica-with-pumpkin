package com.market.banica.calculator.service;

import com.market.banica.calculator.dto.IngredientDTO;
import com.market.banica.calculator.dto.RecipeDTO;
import com.market.banica.calculator.model.Product;
import com.market.banica.calculator.service.contract.CalculatorService;
import com.market.banica.calculator.service.contract.ProductService;
import com.market.banica.calculator.service.grpc.AuroraClientSideService;
import com.orderbook.ItemOrderBookResponse;
import com.orderbook.OrderBookLayer;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Date: 3/10/2021 Time: 5:28 PM
 * <p>
 *
 * @author Vladislav_Zlatanov
 */
@Service
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class CalculatorServiceImpl implements CalculatorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CalculatorServiceImpl.class);

    ProductService recipes;

    AuroraClientSideService auroraService;


    //TODO: UN HARDCODE QUANTITY.
    public RecipeDTO dummyRecipe(String clientId, String itemName, int quantity) {

        List<Product> listProduct = recipes.getProductAsListProduct(itemName);

        LOGGER.info("Received list from product service with size {}", listProduct.size());

        Map<String, List<OrderBookLayer>> ingredients = new HashMap<>();

        for (Product product : listProduct) {
            ItemOrderBookResponse ingredient = auroraService.getIngredient(this.generateAuroraMessage(product, quantity), clientId);

            ingredients.put(ingredient.getItemName(), ingredient.getOrderbookLayersList());
        }

        //evaluate best price for recipe based on products.


        RecipeDTO recipeDTO = new RecipeDTO();
        recipeDTO.setIngredients(dummyIngredients(ingredients));
        recipeDTO.setTotalPrice(recipeDTO.getIngredients().stream()
                .map(IngredientDTO::getPrice)
                .collect(Collectors.toList()).stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        recipeDTO.setItemName(itemName);

        return recipeDTO;
    }


    private Set<IngredientDTO> dummyIngredients(Map<String, List<OrderBookLayer>> ingredients) {
        Set<Map.Entry<String, List<OrderBookLayer>>> entries = ingredients.entrySet();

        Set<IngredientDTO> ingredientDTOSet = new HashSet<>();

        for (Map.Entry<String, List<OrderBookLayer>> entry : entries) {
            IngredientDTO ingredientDTO = new IngredientDTO();

            ingredientDTO.setItemName(entry.getKey());
            ingredientDTO.setLocation(entry.getValue().get(0).getOrigin());
            ingredientDTO.setPrice(BigDecimal.valueOf(entry.getValue().get(0).getPrice()));
            ingredientDTO.setQuantity(entry.getValue().get(0).getQuantity());

            ingredientDTOSet.add(ingredientDTO);
        }
        return ingredientDTOSet;
    }

    private String generateAuroraMessage(Product product, int quatity) {
        return String.format("order-book/%s/%s", product.getProductName(), quatity);
    }

    /**
     * @param itemName name of the item (ex:banica)
     * @param quantity quantity of the item
     * @return recipe for item
     */
    @Override
    public RecipeDTO getRecipe(String clientId, String itemName, int quantity) {

        return dummyRecipe(clientId, itemName, quantity);

//        throw new FeatureNotSupportedException("Feature is not implemented yet.")

        //Due the connection is fake atm.
        //method will fail because of the lack of real connection to aurora service.

        //get recipe from property

        //get desired ingredients name

        //call aurora service for specific ingredient price.

        //IngredientResponse ingredients = auroraService.getIngredient(ingredient, quantity)

    }
}
