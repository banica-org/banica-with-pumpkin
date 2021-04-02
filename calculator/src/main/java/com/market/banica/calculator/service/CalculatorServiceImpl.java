package com.market.banica.calculator.service;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.market.banica.calculator.dto.ProductDto;
import com.market.banica.calculator.dto.ProductSpecification;
import com.market.banica.calculator.model.Product;
import com.market.banica.calculator.service.contract.CalculatorService;
import com.market.banica.calculator.service.contract.ProductService;
import com.market.banica.calculator.service.grpc.AuroraClientSideService;
import com.orderbook.ItemOrderBookResponse;
import com.orderbook.OrderBookLayer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


@Service
@RequiredArgsConstructor
public class CalculatorServiceImpl implements CalculatorService {


    private final AuroraClientSideService auroraService;
    private final ProductService productService;
//    private final TestData testData;

    @Override
    public List<ProductDto> getRecipe(String clientId, String itemName, int quantity) {

        Set<Product> products = productService.getProductAsListProduct(itemName);

        Gson gson = new Gson();
        String jsonString = gson.toJson(products);
        Type type = new TypeToken<HashSet<Product>>() {
        }.getType();
        Set<Product> productsVerifyParentSet = gson.fromJson(jsonString, type);

        List<ProductDto> result = new ArrayList<>();

        Map<String, ProductDto> productDtoMap = new HashMap<>();

        Map<String, List<ProductSpecification>> productSpecificationMap = new HashMap<>();

        for (Product product : products) {

            long tempQuantity = quantity;
            for (Product setProduct : productsVerifyParentSet) {

                if (setProduct.getIngredients().containsKey(product.getProductName())) {

                    tempQuantity = setProduct.getIngredients().get(product.getProductName());
                    setProduct.getIngredients().remove(product.getProductName());
                    break;
                }
            }
            getProductsDataFromOrderBook(clientId, product, tempQuantity, productDtoMap,
                    productSpecificationMap);
        }

        Map<String, ProductDto> compositeProductsDtoMap = new HashMap<>();

        for (Map.Entry<String, ProductDto> entry : productDtoMap.entrySet()) {

            if (!entry.getValue().getIngredients().isEmpty()) {

                compositeProductsDtoMap.put(entry.getKey(), entry.getValue());
            }
        }

        jsonString = gson.toJson(compositeProductsDtoMap);
        type = new TypeToken<HashMap<String, ProductDto>>() {
        }.getType();
        Map<String, ProductDto> compositeProductsDtoVerifyParentMap = gson.fromJson(jsonString, type);

        for (int i = 0; i < compositeProductsDtoMap.size(); i++) {

            ProductDto tempProduct = compositeProductsDtoMap.values().stream()
                    .filter(m -> m.getTotalPrice() == null)
                    .filter(k -> {
                        boolean hasCompositeIngredients = false;
                        for (String ingredientName : k.getIngredients().keySet()) {

                            hasCompositeIngredients = productDtoMap.get(ingredientName).getTotalPrice() != null;
                        }
                        return hasCompositeIngredients;
                    })
                    .findFirst()
                    .orElseThrow(IllegalArgumentException::new);

            long orderedProductQuantity = quantity;
            BigDecimal productPrice = BigDecimal.ZERO;
            BigDecimal ingredientsPrice = BigDecimal.ZERO;

            for (String productDtoName : compositeProductsDtoVerifyParentMap.keySet()) {

                ProductDto productDto = compositeProductsDtoVerifyParentMap.get(productDtoName);

                if (productDto.getIngredients().containsKey(tempProduct.getItemName())) {

                    orderedProductQuantity = productDto.getIngredients().get(tempProduct.getItemName());
                    compositeProductsDtoVerifyParentMap.get(productDto.getItemName()).getIngredients()
                            .remove(tempProduct.getItemName());
                    break;
                }
            }

            productPrice = productPrice.add(checkPriceForProduct(tempProduct,
                    orderedProductQuantity, productSpecificationMap));


            for (String productDtoName : tempProduct.getIngredients().keySet()) {

                BigDecimal tempIngredientPrice = BigDecimal.ZERO;
                ProductDto currentProductDto = productDtoMap.get(productDtoName);
                long ingredientRecipeQuantity = tempProduct.getIngredients().get(productDtoName);

                if (!compositeProductsDtoMap.containsKey(productDtoName)) {

                    tempIngredientPrice = tempIngredientPrice.add(checkPriceForProduct(currentProductDto,
                            ingredientRecipeQuantity, productSpecificationMap));

                } else {

                    tempIngredientPrice = currentProductDto.getTotalPrice();

                }
                ingredientsPrice = ingredientsPrice.add(tempIngredientPrice);

            }


            ingredientsPrice = ingredientsPrice.multiply(BigDecimal.valueOf(orderedProductQuantity));

            if (productPrice.compareTo(ingredientsPrice) > 0) {

                tempProduct.setTotalPrice(ingredientsPrice);

                for (String ingredientName : tempProduct.getIngredients().keySet()) {

                    ProductDto ingredient = productDtoMap.get(ingredientName);

                    if (!result.contains(ingredient)) {
                        result.add(ingredient);
                    }

                    if (!compositeProductsDtoMap.containsKey(ingredientName)) {

                        BigDecimal tempIngredientPrice = BigDecimal.ZERO;

                        tempIngredientPrice = tempIngredientPrice.add(writePriceToProduct(ingredient,
                                tempProduct.getIngredients().get(ingredientName),
                                productSpecificationMap));

                        ingredient.setTotalPrice(ingredient.getTotalPrice().add(tempIngredientPrice));
                    }
                }


            } else {

                tempProduct.setTotalPrice(productPrice);

                for (String ingredientName : tempProduct.getIngredients().keySet()) {
                    ProductDto productDto = productDtoMap.get(ingredientName);
                    if (!productDto.getIngredients().isEmpty()) {
                        for (String tempIngredientName : productDto.getIngredients().keySet()) {
                            ProductDto tempProductDto = productDtoMap.get(tempIngredientName);
                            long ingredientQuantity = productDto.getIngredients().get(tempIngredientName);
                            if (result.contains(tempProductDto)) {
                                ProductSpecification productSpecification = tempProductDto.getProductSpecifications().stream()
                                        .filter(k -> k.getQuantity() == ingredientQuantity)
                                        .findFirst()
                                        .orElseThrow(IllegalArgumentException::new);
                                tempProductDto.getProductSpecifications().remove(productSpecification);
                                if (tempProductDto.getProductSpecifications().isEmpty()) {
                                    result.remove(tempProductDto);
                                }
                            }
                        }
                    }
                    if (result.contains(productDto)){
                        if(productDto.getProductSpecifications().isEmpty() || productDto.getProductSpecifications().size() ==1){
                            result.remove(productDto);
                        }
                    }
                }

                tempProduct.getIngredients().clear();
                writePriceToProduct(tempProduct, orderedProductQuantity, productSpecificationMap);
            }

            result.add(tempProduct);
        }

        if (!compositeProductsDtoMap.get(itemName).getProductSpecifications().isEmpty()) {

            return Collections.singletonList(compositeProductsDtoMap.get(itemName));
        }

        return result;
    }

    private BigDecimal writePriceToProduct(final ProductDto productDto, final long orderedProductQuantity,
                                           final Map<String, List<ProductSpecification>> productSpecificationMap) {

        BigDecimal result = BigDecimal.ZERO;
        long productQuantity = orderedProductQuantity;

        for (ProductSpecification productSpecification : productSpecificationMap.get(productDto.getItemName())) {

            if (productSpecification.getQuantity() == 0) {
                continue;
            }

            long tempQuantity = productSpecification.getQuantity();

            ProductSpecification tempProductSpecification = new ProductSpecification();

            tempProductSpecification.setPrice(productSpecification.getPrice());
            tempProductSpecification.setLocation(productSpecification.getLocation());
            productDto.getProductSpecifications().add(tempProductSpecification);

            if (tempQuantity < productQuantity) {

                productSpecification.setQuantity(0L);
                tempProductSpecification.setQuantity(tempQuantity);
                productQuantity -= tempQuantity;
                result = result.add(BigDecimal.valueOf(tempQuantity)
                        .multiply(productSpecification.getPrice()));

            } else {

                tempProductSpecification.setQuantity(productQuantity);
                productSpecification.setQuantity(tempQuantity - productQuantity);
                result = result.add(BigDecimal.valueOf(productQuantity)
                        .multiply(productSpecification.getPrice()));

                break;
            }
        }
        return result;
    }

    private BigDecimal checkPriceForProduct(final ProductDto productDto, final long orderedProductQuantity,
                                            final Map<String, List<ProductSpecification>> productSpecificationMap) {

        BigDecimal result = BigDecimal.ZERO;
        long productQuantity = orderedProductQuantity;

        for (ProductSpecification productSpecification : productSpecificationMap.get(productDto.getItemName())) {

            long tempQuantity = productSpecification.getQuantity();

            if (tempQuantity < productQuantity) {

                productQuantity -= tempQuantity;
                result = result.add(BigDecimal.valueOf(tempQuantity)
                        .multiply(productSpecification.getPrice()));

            } else {

                result = result.add(BigDecimal.valueOf(productQuantity)
                        .multiply(productSpecification.getPrice()));

                break;
            }
        }
        return result;
    }

    private void getProductsDataFromOrderBook(String clientId, Product product, long quantity, Map<String,
            ProductDto> productDtoMap, Map<String, List<ProductSpecification>> productSpecificationMap) {

        ItemOrderBookResponse orderBookResponse = auroraService.getIngredient(product.getProductName(),clientId,quantity);

        String productName = orderBookResponse.getItemName();

        List<ProductSpecification> productSpecifications = new ArrayList<>();

        for (OrderBookLayer layer : orderBookResponse.getOrderbookLayersList()) {

            ProductSpecification productSpecification = createProductSpecification(layer);
            productSpecifications.add(productSpecification);
        }

        productSpecificationMap.put(productName, productSpecifications);

        ProductDto productDto = new ProductDto();
        productDto.setItemName(productName);

        if (product.getIngredients().isEmpty()) {

            productDto.setTotalPrice(BigDecimal.ZERO);

        } else {

            productDto.setIngredients(product.getIngredients());
        }
        productDtoMap.put(productDto.getItemName(), productDto);
    }

    private ProductSpecification createProductSpecification(OrderBookLayer layer) {

        ProductSpecification productSpecification = new ProductSpecification();

        productSpecification.setPrice(BigDecimal.valueOf(layer.getPrice()));
        productSpecification.setQuantity(layer.getQuantity());
        productSpecification.setLocation(layer.getOrigin().toString());

        return productSpecification;
    }
}
