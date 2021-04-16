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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class CalculatorServiceImpl implements CalculatorService {

    private final AuroraClientSideService auroraService;
    private final ProductService productService;
    private final TestData testData;

    @Override
    public List<ProductDto> getRecipe(String clientId, String itemName, long quantity) {

        Map<Product, List<Long>> products = productService.getProductIngredientsWithQuantity(itemName);

        Map<ProductDto, List<Long>> productDtoMap = new HashMap<>();

        Map<String, List<ProductSpecification>> productSpecificationMap = new HashMap<>();

        populateOrderedProductDataToDataStructures(clientId, itemName, quantity,
                products, productDtoMap, productSpecificationMap);

        populateConstituentProductsDataToDataStructures(clientId, products,
                productDtoMap, productSpecificationMap);

        Map<String, ProductDto> productDtoNamesMap = productDtoMap.keySet().stream()
                .collect(Collectors.toMap(ProductDto::getItemName, Function.identity()));

        List<ProductDto> result = new ArrayList<>();

        Gson gson = new Gson();
        Type type = new TypeToken<HashMap<String, List<ProductSpecification>>>() {
        }.getType();

        String productSpecificationJsonString = gson.toJson(productSpecificationMap);

        boolean isCompositeProductWithCompositeIngredientsSetAsSimpleProduct = true;
        while (isCompositeProductWithCompositeIngredientsSetAsSimpleProduct) {

            result = new ArrayList<>();

            isCompositeProductWithCompositeIngredientsSetAsSimpleProduct = checkBestPrices(
                    productDtoMap, productSpecificationMap,
                    productDtoNamesMap, result);


            productSpecificationMap = gson.fromJson(productSpecificationJsonString,type);
        }

        calculateBestPrices(productDtoMap, productSpecificationMap,
                productDtoNamesMap, result);

        if (!productDtoNamesMap.get(itemName).getProductSpecifications().isEmpty()) {

            result = Arrays.asList(productDtoNamesMap.get(itemName));
        }

        Collections.reverse(result);

        return result;
    }

    private void calculateBestPrices(Map<ProductDto, List<Long>> productDtoMap,
                                     Map<String, List<ProductSpecification>> productSpecificationMap,
                                     Map<String, ProductDto> productDtoNamesMap, List<ProductDto> result) {

        for (ProductDto tempProduct : result) {

            if (tempProduct.getIngredients().isEmpty()) continue;

            for (long tempQuantity : productDtoMap.get(tempProduct)) {

                BigDecimal ingredientsPrice = BigDecimal.ZERO;

                ingredientsPrice = calculateIngredientsPrice(productSpecificationMap,
                        productDtoNamesMap, tempProduct, ingredientsPrice);

                ingredientsPrice = ingredientsPrice.multiply(BigDecimal.valueOf(tempQuantity));

                tempProduct.setTotalPrice(ingredientsPrice);

                for (String ingredientName : tempProduct.getIngredients().keySet()) {

                    ProductDto ingredient = productDtoNamesMap.get(ingredientName);

                    if (ingredient.getIngredients().isEmpty()) {

                        BigDecimal tempIngredientPrice = BigDecimal.ZERO;

                        tempIngredientPrice = tempIngredientPrice.add(writePriceToProduct(ingredient,
                                tempProduct.getIngredients().get(ingredientName),
                                productSpecificationMap));

                        ingredient.setTotalPrice(ingredient.getTotalPrice().add(tempIngredientPrice));
                    }
                }
            }
        }
    }

    private boolean checkBestPrices(Map<ProductDto, List<Long>> productDtoMap,
                                    Map<String, List<ProductSpecification>> productSpecificationMap,
                                    Map<String, ProductDto> productDtoNamesMap, List<ProductDto> result) {

        while(true) {

            Optional<ProductDto> optionalProductDto = getNotCompositeProductWhichIngredientsDoesNotHaveIngredients
                    (productDtoNamesMap, productDtoMap);

            if (!optionalProductDto.isPresent()) break;

            ProductDto tempProduct = optionalProductDto.get();

            for (long tempQuantity : productDtoMap.get(tempProduct)) {

                BigDecimal productPrice = BigDecimal.ZERO;
                BigDecimal ingredientsPrice = BigDecimal.ZERO;

                productPrice = productPrice.add(checkPriceForProduct(tempProduct,
                        tempQuantity, productSpecificationMap));

                ingredientsPrice = calculateIngredientsPrice(productSpecificationMap,
                        productDtoNamesMap, tempProduct, ingredientsPrice);


                ingredientsPrice = ingredientsPrice.multiply(BigDecimal.valueOf(tempQuantity));


                if (productPrice.compareTo(ingredientsPrice) > 0) {

                    tempProduct.setTotalPrice(ingredientsPrice);

                    for (String ingredientName : tempProduct.getIngredients().keySet()) {

                        ProductDto ingredient = productDtoNamesMap.get(ingredientName);

                        if (!result.contains(ingredient)) {

                            result.add(ingredient);
                        }

                        if (ingredient.getIngredients().isEmpty()) {

                            reserveProductQuantities(ingredient,
                                    tempProduct.getIngredients().get(ingredientName),
                                    productSpecificationMap);

                        }
                    }
                } else {
                    tempProduct.setTotalPrice(BigDecimal.ZERO);

                    for (String ingredientName : tempProduct.getIngredients().keySet()) {

                        ProductDto ingredient = productDtoNamesMap.get(ingredientName);
                        if (!ingredient.getIngredients().isEmpty()) {

                            //should be done in a dfs
                            productDtoMap.get(ingredient).remove(tempProduct.getIngredients().get(ingredientName));
                            tempProduct.getIngredients().clear();

                            return true;
                        }
                    }

                    tempProduct.getIngredients().clear();

                    reserveProductQuantities(tempProduct, tempQuantity, productSpecificationMap);
                }
                result.add(tempProduct);
            }
        }
        return false;
    }

    private Optional<ProductDto> getNotCompositeProductWhichIngredientsDoesNotHaveIngredients(Map<String, ProductDto> productDtoNamesMap,
                                                                                              Map<ProductDto, List<Long>> productDtoMap) {

        return productDtoMap.keySet().stream().filter(
                m -> !m.getIngredients().isEmpty() && m.getTotalPrice() == null &&
                        m.getIngredients().keySet()
                                .stream()
                                .allMatch(productName -> productDtoNamesMap.get(productName).getTotalPrice() != null))
                .findFirst();
    }

    private BigDecimal calculateIngredientsPrice(Map<String, List<ProductSpecification>> productSpecificationMap,
                                                 Map<String, ProductDto> productDtoNamesMap, ProductDto tempProduct,
                                                 BigDecimal ingredientsPrice) {

        for (String ingredientName : tempProduct.getIngredients().keySet()) {

            BigDecimal tempIngredientPrice = BigDecimal.ZERO;
            ProductDto currentIngredient = productDtoNamesMap.get(ingredientName);
            long ingredientRecipeQuantity = tempProduct.getIngredients().get(ingredientName);

            if (currentIngredient.getTotalPrice().equals(BigDecimal.ZERO)) {

                tempIngredientPrice = tempIngredientPrice.add(checkPriceForProduct(currentIngredient,
                        ingredientRecipeQuantity, productSpecificationMap));

            } else {

                tempIngredientPrice = currentIngredient.getTotalPrice();

            }
            ingredientsPrice = ingredientsPrice.add(tempIngredientPrice);

        }
        return ingredientsPrice;
    }

    private void populateConstituentProductsDataToDataStructures(String clientId, Map<Product, List<Long>> products,
                                                                 Map<ProductDto, List<Long>> productDtoMap,
                                                                 Map<String, List<ProductSpecification>> productSpecificationMap) {

        for (Product product : products.keySet()) {

            long tempQuantity = products.get(product).stream().mapToLong(Long::longValue).sum();

            getProductsMarketDataFromOrderBook(clientId, product, tempQuantity,
                    productSpecificationMap);

            convertProductToProductDto(product, products, productDtoMap);
        }
    }

    private void populateOrderedProductDataToDataStructures(String clientId, String itemName, long quantity,
                                                            Map<Product, List<Long>> products, Map<ProductDto,
            List<Long>> productDtoMap, Map<String, List<ProductSpecification>> productSpecificationMap) {

        Product orderedProduct = productService.getProductFromDatabase(itemName);

        products.put(orderedProduct, new ArrayList<>(Arrays.asList(quantity)));

        getProductsMarketDataFromOrderBook(clientId, orderedProduct, quantity,
                productSpecificationMap);
        convertProductToProductDto(orderedProduct, products, productDtoMap);
    }

    private void convertProductToProductDto(Product product, Map<Product, List<Long>> products,
                                            Map<ProductDto, List<Long>> productDtoMap) {

        ProductDto productDto = new ProductDto();

        productDto.setItemName(product.getProductName());

        if (product.getIngredients().isEmpty()) {

            productDto.setTotalPrice(BigDecimal.ZERO);

        } else {

            productDto.setIngredients(product.getIngredients());
        }

        productDtoMap.put(productDto, products.get(product));
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

            ProductSpecification tempProductSpecification = createProductSpecificationFromExisting(productSpecification);

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

    private ProductSpecification createProductSpecificationFromExisting(ProductSpecification productSpecification) {

        ProductSpecification tempProductSpecification = new ProductSpecification();
        tempProductSpecification.setPrice(productSpecification.getPrice());
        tempProductSpecification.setLocation(productSpecification.getLocation());

        return tempProductSpecification;
    }

    private void reserveProductQuantities(final ProductDto productDto, final long orderedProductQuantity,
                                          final Map<String, List<ProductSpecification>> productSpecificationMap) {

        long productQuantity = orderedProductQuantity;

        for (ProductSpecification productSpecification : productSpecificationMap.get(productDto.getItemName())) {

            if (productSpecification.getQuantity() == 0) {
                continue;
            }

            long tempQuantity = productSpecification.getQuantity();

            if (tempQuantity < productQuantity) {

                productSpecification.setQuantity(0L);
                productQuantity -= tempQuantity;

            } else {

                productSpecification.setQuantity(tempQuantity - productQuantity);

                break;
            }
        }
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

    private void getProductsMarketDataFromOrderBook(String clientId, Product product, long quantity, Map<String, List<ProductSpecification>> productSpecificationMap) {

        ItemOrderBookResponse orderBookResponse = testData.getTestData1().get(product.getProductName());
//        ItemOrderBookResponse orderBookResponse = auroraService.getIngredient(product.getProductName(),clientId,quantity);

        String productName = orderBookResponse.getItemName();

        List<ProductSpecification> productSpecifications = new ArrayList<>();

        for (OrderBookLayer layer : orderBookResponse.getOrderbookLayersList()) {

            ProductSpecification productSpecification = createProductSpecification(layer);
            productSpecifications.add(productSpecification);
        }

        productSpecificationMap.put(productName, productSpecifications);
    }

    private ProductSpecification createProductSpecification(OrderBookLayer layer) {

        ProductSpecification productSpecification = new ProductSpecification();

        productSpecification.setPrice(BigDecimal.valueOf(layer.getPrice()));
        productSpecification.setQuantity(layer.getQuantity());
        productSpecification.setLocation(layer.getOrigin().toString());

        return productSpecification;
    }
}
