package com.market.banica.calculator.service;

import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.market.banica.calculator.dto.ProductDto;
import com.market.banica.calculator.dto.ProductPriceComponentsSet;
import com.market.banica.calculator.dto.ProductSpecification;
import com.market.banica.common.exception.ProductNotAvailableException;
import com.market.banica.calculator.model.Pair;
import com.market.banica.calculator.model.Product;
import com.market.banica.calculator.service.contract.CalculatorService;
import com.market.banica.calculator.service.contract.ProductService;
import com.market.banica.calculator.service.grpc.AuroraClientSideService;
import com.orderbook.ItemOrderBookResponse;
import com.orderbook.OrderBookLayer;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class CalculatorServiceImpl implements CalculatorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CalculatorServiceImpl.class);

    private final AuroraClientSideService auroraService;
    private final ProductService productService;

    @Override
    public List<ProductDto> getProduct(String clientId, String itemName, long quantity) throws ProductNotAvailableException {
        LOGGER.debug("In getProduct method");

        Map<Product, Map<String, Pair<Long, Long>>> products = productService.getProductIngredientsWithQuantityPerParent(itemName, quantity);
        Map<ProductDto, Map<String, Pair<Long, Long>>> productDtoMap = new HashMap<>();
        Map<String, List<ProductSpecification>> productSpecificationMap = new HashMap<>();

        populateDataToDataStructures(clientId, itemName, quantity, products,
                productDtoMap, productSpecificationMap);

        Map<String, ProductDto> productDtoNamesMap = productDtoMap.keySet().stream()
                .collect(Collectors.toMap(ProductDto::getItemName, Function.identity()));
        Map<Integer, ProductPriceComponentsSet> productPriceComponentsSetByProductIdMap = new HashMap<>();

        ProductPriceComponentsSet result = createBestPricePath(productDtoMap,
                productSpecificationMap, productDtoNamesMap,
                productPriceComponentsSetByProductIdMap, itemName);

        return writeProductsFromBestPricePath(productSpecificationMap,
                productDtoNamesMap, result,
                productPriceComponentsSetByProductIdMap);
    }

    private void populateDataToDataStructures(String clientId, String itemName, long quantity,
                                              Map<Product, Map<String, Pair<Long, Long>>> products,
                                              Map<ProductDto, Map<String, Pair<Long, Long>>> productDtoMap,
                                              Map<String, List<ProductSpecification>> productSpecificationMap) {
        LOGGER.debug("In populateDataToDataStructures private method");

        populateConstituentProductsDataToDataStructures(clientId, products,
                productDtoMap, productSpecificationMap);

        populateOrderedProductDataToDataStructures(clientId, itemName, quantity,
                products, productDtoMap, productSpecificationMap);
    }

    private List<ProductDto> writeProductsFromBestPricePath(Map<String, List<ProductSpecification>> productSpecificationMap,
                                                            Map<String, ProductDto> productDtoNamesMap,
                                                            ProductPriceComponentsSet resultProductPriceComponentsSet,
                                                            Map<Integer, ProductPriceComponentsSet> productPriceComponentsSetByProductIdMap) {
        LOGGER.debug("In writeProductsFromBestPricePath private method");

        List<ProductDto> result = new ArrayList<>();

        if (resultProductPriceComponentsSet.getComponentIngredients().isEmpty()) {
            result = Collections.singletonList(createSimpleParentProduct(productSpecificationMap,
                    resultProductPriceComponentsSet));
        } else {
            createListOfProductsWhenParentIsComposite(productSpecificationMap, productDtoNamesMap,
                    resultProductPriceComponentsSet, productPriceComponentsSetByProductIdMap,
                    result);
        }

        return result;
    }

    private void createListOfProductsWhenParentIsComposite(Map<String, List<ProductSpecification>> productSpecificationMap,
                                                           Map<String, ProductDto> productDtoNamesMap,
                                                           ProductPriceComponentsSet resultProductPriceComponentsSet,
                                                           Map<Integer, ProductPriceComponentsSet> productPriceComponentsSetByProductIdMap,
                                                           List<ProductDto> result) {
        LOGGER.debug("In createListOfProductsWhenParentIsComposite private method");

        ProductDto parentProduct = productDtoNamesMap.get(resultProductPriceComponentsSet.getProductName());
        ProductDto newParentProduct = createParentProduct(resultProductPriceComponentsSet,
                parentProduct);

        result.add(newParentProduct);

        createIngredientProducts(productSpecificationMap, productDtoNamesMap,
                resultProductPriceComponentsSet, productPriceComponentsSetByProductIdMap,
                result);
    }

    private void createIngredientProducts(Map<String, List<ProductSpecification>> productSpecificationMap,
                                          Map<String, ProductDto> productDtoNamesMap,
                                          ProductPriceComponentsSet resultProductPriceComponentsSet,
                                          Map<Integer, ProductPriceComponentsSet> productPriceComponentsSetByProductIdMap,
                                          List<ProductDto> result) {
        LOGGER.debug("In createIngredientProducts private method");

        for (String tempProductName : resultProductPriceComponentsSet.getComponentIngredients().keySet()) {
            ProductDto productDto = productDtoNamesMap.get(tempProductName);
            for (Integer productId : resultProductPriceComponentsSet.getComponentIngredients().get(tempProductName)) {
                ProductPriceComponentsSet productPriceComponentsSet = productPriceComponentsSetByProductIdMap.get(productId);

                ProductDto newProductDto = new ProductDto();
                newProductDto.setItemName(productDto.getItemName());

                if (!productPriceComponentsSet.getComponentIngredients().isEmpty()) {

                    newProductDto.setTotalPrice(productPriceComponentsSet.getPrice());
                    newProductDto.setIngredients(productDto.getIngredients());

                    result.add(newProductDto);
                    continue;
                }

                createProductSpecificationsForSimpleProduct(productSpecificationMap, tempProductName,
                        productPriceComponentsSet, newProductDto);

                newProductDto.setTotalPrice(productPriceComponentsSet.getPrice());
                result.add(newProductDto);
            }
        }
    }

    private ProductDto createParentProduct(ProductPriceComponentsSet resultProductPriceComponentsSet,
                                           ProductDto parentProduct) {
        LOGGER.debug("In createParentProduct private method");

        ProductDto newParentProduct = new ProductDto();

        newParentProduct.setItemName(resultProductPriceComponentsSet.getProductName());
        newParentProduct.setTotalPrice(resultProductPriceComponentsSet.getPrice());
        newParentProduct.setIngredients(parentProduct.getIngredients());

        return newParentProduct;
    }

    private ProductDto createSimpleParentProduct(Map<String, List<ProductSpecification>> productSpecificationMap,
                                                 ProductPriceComponentsSet resultProductPriceComponentsSet) {
        LOGGER.debug("In createSimpleParentProduct private method");

        ProductDto newParentProduct = new ProductDto();

        newParentProduct.setItemName(resultProductPriceComponentsSet.getProductName());
        newParentProduct.setTotalPrice(resultProductPriceComponentsSet.getPrice());

        createProductSpecificationsForSimpleProduct(productSpecificationMap,
                resultProductPriceComponentsSet.getProductName(),
                resultProductPriceComponentsSet, newParentProduct);

        return newParentProduct;
    }

    private void createProductSpecificationsForSimpleProduct(Map<String, List<ProductSpecification>> productSpecificationMap,
                                                             String tempProductName,
                                                             ProductPriceComponentsSet productPriceComponentsSet,
                                                             ProductDto newProductDto) {
        LOGGER.debug("In createProductSpecificationsForSimpleProduct private method");

        long start = productPriceComponentsSet.getReservedQuantityRangeStartEnd().getFirst();
        long range = productPriceComponentsSet.getReservedQuantityRangeStartEnd().getSecond() -
                productPriceComponentsSet.getReservedQuantityRangeStartEnd().getFirst();

        for (ProductSpecification productSpecification : productSpecificationMap.get(tempProductName)) {
            ProductSpecification newProductSpecification = new ProductSpecification();
            if (start > productSpecification.getQuantity()) {
                start -= productSpecification.getQuantity();
                continue;
            }
            if (range > productSpecification.getQuantity() - start) {

                newProductSpecification.setLocation(productSpecification.getLocation());
                newProductSpecification.setQuantity(productSpecification.getQuantity() - start);
                newProductSpecification.setPrice(productSpecification.getPrice());

                newProductDto.getProductSpecifications().add(newProductSpecification);

                range -= productSpecification.getQuantity() - start;
                start = 0;
                continue;
            }

            newProductSpecification.setLocation(productSpecification.getLocation());
            newProductSpecification.setQuantity(range);
            newProductSpecification.setPrice(productSpecification.getPrice());

            newProductDto.getProductSpecifications().add(newProductSpecification);

            break;
        }
    }

    private ProductPriceComponentsSet createBestPricePath(Map<ProductDto, Map<String, Pair<Long, Long>>> productDtoMap,
                                                          Map<String, List<ProductSpecification>> productSpecificationMap,
                                                          Map<String, ProductDto> productDtoNamesMap,
                                                          Map<Integer, ProductPriceComponentsSet> productPriceComponentsSetByProductIdMap,
                                                          String itemName) throws ProductNotAvailableException {
        LOGGER.debug("In createBestPricePath private method");

        List<ProductDto> compositeProductDtoList = createListWithOnlyCompositeProducts(productDtoMap);
        Map<String, List<List<ProductPriceComponentsSet>>> result = new HashMap<>();

        createPriceVariantsForProducts(productDtoMap, productSpecificationMap,
                productDtoNamesMap, productPriceComponentsSetByProductIdMap,
                compositeProductDtoList, result);

        ProductDto orderedProduct = productDtoNamesMap.get(itemName);
        long orderedQuantity = productDtoMap.get(orderedProduct).get(itemName).getSecond();

        List<ProductPriceComponentsSet> orderedProductPriceVariantsSet =
                createPriceVariantsForOrderedProduct(productDtoMap, productSpecificationMap,
                        productPriceComponentsSetByProductIdMap, result,
                        orderedProduct, orderedQuantity);
        Set<ProductPriceComponentsSet> resultSet = new TreeSet<>(orderedProductPriceVariantsSet);

        if (resultSet.isEmpty()) {
            throw new ProductNotAvailableException(String.format("Product %s not available on" +
                    " the market", itemName));
        }
        return resultSet.iterator().next();
    }

    private void createPriceVariantsForProducts(Map<ProductDto, Map<String, Pair<Long, Long>>> productDtoMap,
                                                Map<String, List<ProductSpecification>> productSpecificationMap,
                                                Map<String, ProductDto> productDtoNamesMap,
                                                Map<Integer, ProductPriceComponentsSet> productPriceComponentsSetByProductIdMap,
                                                List<ProductDto> compositeProductDtoList,
                                                Map<String, List<List<ProductPriceComponentsSet>>> result) {
        LOGGER.debug("In createPriceVariantsForProducts private method");

        while (true) {

            Optional<ProductDto> optionalProductDto = getProductWhichIngredientsAreNotCompositeOrIsChecked
                    (productDtoNamesMap, compositeProductDtoList);
            if (!optionalProductDto.isPresent()) {
                break;
            }
            ProductDto tempProduct = optionalProductDto.get();

            List<List<ProductPriceComponentsSet>> ingredientsPriceVariantsLists = createPriceVariantsForIngredients(productDtoMap,
                    productSpecificationMap, productDtoNamesMap,
                    productPriceComponentsSetByProductIdMap, result, tempProduct);

            List<List<ProductPriceComponentsSet>> cartesianProductOfIngredientsPriceVariants =
                    Lists.cartesianProduct(ingredientsPriceVariantsLists);

            List<List<ProductPriceComponentsSet>> compositeProductPriceVariantList = createCompositeProductPriceVariantsFromIngredientsPriceVariants(
                    productPriceComponentsSetByProductIdMap,
                    tempProduct, cartesianProductOfIngredientsPriceVariants);

            markCurrentProductAsChecked(tempProduct);
            result.put(tempProduct.getItemName(), compositeProductPriceVariantList);
        }
    }

    private void markCurrentProductAsChecked(ProductDto tempProduct) {
        LOGGER.debug("In markCurrentProductAsChecked private method");

        tempProduct.setTotalPrice(BigDecimal.valueOf(-1));
    }

    private List<List<ProductPriceComponentsSet>> createCompositeProductPriceVariantsFromIngredientsPriceVariants(Map<Integer, ProductPriceComponentsSet> productPriceComponentsSetByProductIdMap,
                                                                                                                  ProductDto tempProduct,
                                                                                                                  List<List<ProductPriceComponentsSet>> cartesianProductOfIngredientsPriceVariants) {
        LOGGER.debug("In createCompositeProductPriceVariantsFromIngredientsPriceVariants private method");

        List<List<ProductPriceComponentsSet>> compositeProductPriceVariantList = new ArrayList<>();
        conflictOfIngredientsQuantitiesRanges:
        for (List<ProductPriceComponentsSet> ingredientsList : cartesianProductOfIngredientsPriceVariants) {

            List<ProductPriceComponentsSet> compositeProductPriceVariant = new ArrayList<>();
            Map<String, List<Integer>> componentIngredientsNamesMap = new HashMap<>();

            BigDecimal subPrice = BigDecimal.ZERO;
            for (ProductPriceComponentsSet ingredientPriceVariant : ingredientsList) {
                Map<ProductPriceComponentsSet, ProductPriceComponentsSet> sameIngredientsMap =
                        getMapOfExistingIngredientsSameAsCandidateOrContainedFromCandidate
                                (compositeProductPriceVariant, ingredientPriceVariant,
                                        productPriceComponentsSetByProductIdMap);

                if (isConflictWhenOverlappingRangeOfSameIngredientsQuantities(sameIngredientsMap)) {
                    continue conflictOfIngredientsQuantitiesRanges;
                }

                subPrice = subPrice.add(ingredientPriceVariant.getPrice());
                componentIngredientsNamesMap.put(ingredientPriceVariant.getProductName(), Arrays.asList(ingredientPriceVariant.getProductId()));

                if (!ingredientPriceVariant.getComponentIngredients().isEmpty()) {
                    for (String ingredientName : ingredientPriceVariant.getComponentIngredients().keySet()) {
                        componentIngredientsNamesMap.merge(ingredientName,
                                ingredientPriceVariant.getComponentIngredients().get(ingredientName),
                                (productId1, productId2) ->
                                        Stream.concat(productId1.stream(), productId2.stream())
                                                .collect(Collectors.toList()));
                    }
                }
                compositeProductPriceVariant.add(ingredientPriceVariant);
            }

            if (componentIngredientsNamesMap.isEmpty()) {
                continue;
            }

            ProductPriceComponentsSet product = createCompositeProductPriceComponentsSet(tempProduct,
                    subPrice, componentIngredientsNamesMap,
                    productPriceComponentsSetByProductIdMap);

            compositeProductPriceVariant.add(product);
            compositeProductPriceVariantList.add(compositeProductPriceVariant);
        }
        return compositeProductPriceVariantList;
    }

    private List<List<ProductPriceComponentsSet>> createPriceVariantsForIngredients(Map<ProductDto, Map<String, Pair<Long, Long>>> productDtoMap,
                                                                                    Map<String, List<ProductSpecification>> productSpecificationMap,
                                                                                    Map<String, ProductDto> productDtoNamesMap,
                                                                                    Map<Integer, ProductPriceComponentsSet> productPriceComponentsSetByProductIdMap,
                                                                                    Map<String, List<List<ProductPriceComponentsSet>>> result,
                                                                                    ProductDto tempProduct) {
        LOGGER.debug("In createPriceVariantsForIngredients private method");

        List<List<ProductPriceComponentsSet>> ingredientsPriceVariantsLists = new ArrayList<>();
        for (String ingredientName : tempProduct.getIngredients().keySet()) {
            ProductDto ingredient = productDtoNamesMap.get(ingredientName);
            if (ingredient.getTotalPrice().equals(BigDecimal.valueOf(-1))) {
                List<ProductPriceComponentsSet> compositeIngredientPriceVariants =
                        createPriceVariantsForCompositeProduct(productDtoMap, productSpecificationMap,
                                productPriceComponentsSetByProductIdMap, result,
                                tempProduct, ingredient);

                ingredientsPriceVariantsLists.add(compositeIngredientPriceVariants);
                continue;
            }

            Set<ProductPriceComponentsSet> ingredientPriceVariantsSet =
                    calculatePossiblePricesForProductFromProductSpecifications(productDtoMap,
                            productSpecificationMap, tempProduct, ingredient,
                            productPriceComponentsSetByProductIdMap);

            if (ingredientPriceVariantsSet.isEmpty()) {
                return new ArrayList<>();
            }
            ingredientsPriceVariantsLists.add(new ArrayList<>(ingredientPriceVariantsSet));
        }
        return ingredientsPriceVariantsLists;
    }

    private List<ProductPriceComponentsSet> createPriceVariantsForOrderedProduct(Map<ProductDto, Map<String, Pair<Long, Long>>> productDtoMap,
                                                                                 Map<String, List<ProductSpecification>> productSpecificationMap,
                                                                                 Map<Integer, ProductPriceComponentsSet> productPriceComponentsSetByProductIdMap,
                                                                                 Map<String, List<List<ProductPriceComponentsSet>>> result,
                                                                                 ProductDto orderedProduct, long quantity) {
        LOGGER.debug("In createPriceVariantsForOrderedProduct private method");

        Set<ProductPriceComponentsSet> simpleProductPriceVariantsSet = calculatePossiblePricesForProductFromProductSpecifications(
                productDtoMap, productSpecificationMap, orderedProduct,
                orderedProduct, productPriceComponentsSetByProductIdMap);

        List<ProductPriceComponentsSet> compositeIngredientPriceVariants = new ArrayList<>();

        if (result.containsKey(orderedProduct.getItemName())) {
            List<List<ProductPriceComponentsSet>> compositeProductPriceVariantSets = result.get(orderedProduct.getItemName());

            compositeIngredientPriceVariants = compositeProductPriceVariantSets
                    .stream()
                    .flatMap(Collection::stream)
                    .filter(priceComponentsSet -> priceComponentsSet.getProductName().equals(orderedProduct.getItemName()))
                    .collect(Collectors.toList());

            compositeIngredientPriceVariants.forEach(priceComponentsSet -> {
                BigDecimal newPrice = priceComponentsSet.getPrice().multiply(BigDecimal.valueOf(quantity));
                priceComponentsSet.setPrice(newPrice);
            });
        }
        compositeIngredientPriceVariants.addAll(simpleProductPriceVariantsSet);
        return compositeIngredientPriceVariants;
    }

    private List<ProductPriceComponentsSet> createPriceVariantsForCompositeProduct(Map<ProductDto, Map<String, Pair<Long, Long>>> productDtoMap,
                                                                                   Map<String, List<ProductSpecification>> productSpecificationMap,
                                                                                   Map<Integer, ProductPriceComponentsSet> productPriceComponentsSetByProductIdMap,
                                                                                   Map<String, List<List<ProductPriceComponentsSet>>> result,
                                                                                   ProductDto tempProduct, ProductDto ingredient) {
        LOGGER.debug("In createPriceVariantsForCompositeProduct private method");

        Set<ProductPriceComponentsSet> simpleProductPriceVariantsSet = calculatePossiblePricesForProductFromProductSpecifications(
                productDtoMap, productSpecificationMap, tempProduct,
                ingredient, productPriceComponentsSetByProductIdMap);

        simpleProductPriceVariantsSet.forEach(priceComponentsSet -> {
            BigDecimal newPrice = priceComponentsSet.getPrice()
                    .divide(BigDecimal.valueOf(tempProduct.getIngredients().get(ingredient.getItemName())),
                            RoundingMode.HALF_UP);
            priceComponentsSet.setPrice(newPrice);
        });

        List<List<ProductPriceComponentsSet>> compositeProductPriceVariantSets = result.get(ingredient.getItemName());

        compositeProductPriceVariantSets.add(new ArrayList<>(simpleProductPriceVariantsSet));

        List<ProductPriceComponentsSet> compositeIngredientPriceVariants = compositeProductPriceVariantSets
                .stream()
                .flatMap(Collection::stream)
                .filter(priceComponentsSet -> priceComponentsSet.getProductName().equals(ingredient.getItemName()))
                .collect(Collectors.toList());

        compositeIngredientPriceVariants.forEach(priceComponentsSet -> {
            BigDecimal newPrice = priceComponentsSet.getPrice().multiply(BigDecimal.valueOf(tempProduct.getIngredients().get(ingredient.getItemName())));
            priceComponentsSet.setPrice(newPrice);
        });
        return compositeIngredientPriceVariants;
    }

    private List<ProductDto> createListWithOnlyCompositeProducts(Map<ProductDto, Map<String, Pair<Long, Long>>> productDtoMap) {
        LOGGER.debug("In createListWithOnlyCompositeProducts private method");

        List<ProductDto> compositeProductDtoList = new ArrayList<>();
        for (ProductDto productDto : productDtoMap.keySet()) {
            if (productDto.getIngredients().isEmpty()) {
                continue;
            }
            compositeProductDtoList.add(productDto);
        }
        return compositeProductDtoList;
    }

    private Set<ProductPriceComponentsSet> calculatePossiblePricesForProductFromProductSpecifications(Map<ProductDto, Map<String, Pair<Long, Long>>> productDtoMap,
                                                                                                      Map<String, List<ProductSpecification>> productSpecificationMap,
                                                                                                      ProductDto parentProduct,
                                                                                                      ProductDto ingredient,
                                                                                                      Map<Integer, ProductPriceComponentsSet> productPriceComponentsSetByProductIdMap) {
        LOGGER.debug("In calculatePossiblePricesForProductFromProductSpecifications private method");

        Collection<List<Pair<String, Pair<Long, Long>>>> permutationsOfQuantitiesOfProductSet =
                createPermutationsOfAvailableQuantitiesOfIngredient(productDtoMap, ingredient);

        return createPossibleProductPriceComponentSetsForProduct(
                productSpecificationMap, parentProduct, ingredient.getItemName(),
                permutationsOfQuantitiesOfProductSet,
                productPriceComponentsSetByProductIdMap);
    }

    private Set<ProductPriceComponentsSet> createPossibleProductPriceComponentSetsForProduct(Map<String, List<ProductSpecification>> productSpecificationMap,
                                                                                             ProductDto tempProduct, String ingredientName,
                                                                                             Collection<List<Pair<String, Pair<Long, Long>>>> permutationsOfQuantitiesOfProductSet,
                                                                                             Map<Integer, ProductPriceComponentsSet> productPriceComponentsSetByProductIdMap) {
        LOGGER.debug("In createPossibleProductPriceComponentSetsForProduct private method");

        Set<ProductPriceComponentsSet> ingredientPriceVariantsSet = new HashSet<>();
        for (List<Pair<String, Pair<Long, Long>>> permutationsOfQuantities : permutationsOfQuantitiesOfProductSet) {
            List<ProductSpecification> deepCopyOfProductSpecification =
                    getDeepCopyOfProductSpecifications(productSpecificationMap.get(ingredientName));

            createSingleProductPriceComponentSetAsIngredientPriceVariant(tempProduct, ingredientName,
                    ingredientPriceVariantsSet, permutationsOfQuantities,
                    deepCopyOfProductSpecification,
                    productPriceComponentsSetByProductIdMap);
        }
        return ingredientPriceVariantsSet;
    }

    private void createSingleProductPriceComponentSetAsIngredientPriceVariant(ProductDto tempProduct,
                                                                              String ingredientName,
                                                                              Set<ProductPriceComponentsSet> ingredientPriceVariantsSet,
                                                                              List<Pair<String, Pair<Long, Long>>> permutationsOfQuantities,
                                                                              List<ProductSpecification> deepCopyOfProductSpecification,
                                                                              Map<Integer, ProductPriceComponentsSet> productPriceComponentsSetByProductIdMap) {
        LOGGER.debug("In createSingleProductPriceComponentSetAsIngredientPriceVariant private method");

        for (Pair<String, Pair<Long, Long>> permutationsOfQuantity : permutationsOfQuantities) {
            if (permutationsOfQuantity.getFirst().equals(tempProduct.getItemName())) {
                BigDecimal productPrice = checkPriceForProduct(
                        permutationsOfQuantity.getSecond(),
                        deepCopyOfProductSpecification);
                if (productPrice.equals(BigDecimal.valueOf(0.0))) {
                    break;
                }

                long predecessorsQuantitySum = calculatePredecessorsQuantitySum(permutationsOfQuantities,
                        permutationsOfQuantity);

                ProductPriceComponentsSet ingredientPriceVariant = createNotCompositeProductPriceComponentsSet(ingredientName,
                        permutationsOfQuantity, productPrice,
                        predecessorsQuantitySum,
                        productPriceComponentsSetByProductIdMap);

                ingredientPriceVariantsSet.add(ingredientPriceVariant);
                break;
            }

            reserveProductQuantities(permutationsOfQuantity.getSecond().getSecond(),
                    deepCopyOfProductSpecification);
        }
    }

    private long calculatePredecessorsQuantitySum(List<Pair<String, Pair<Long, Long>>> permutationsOfQuantities,
                                                  Pair<String, Pair<Long, Long>> permutationsOfQuantity) {
        LOGGER.debug("In calculatePredecessorsQuantitySum private method");

        long predecessorsQuantitySum = 0;
        for (Pair<String, Pair<Long, Long>> permutationsOfQuantityParent : permutationsOfQuantities) {
            if (permutationsOfQuantityParent.equals(permutationsOfQuantity)) {
                break;
            }
            predecessorsQuantitySum += permutationsOfQuantityParent.getSecond().getSecond();
        }
        return predecessorsQuantitySum;
    }

    private Collection<List<Pair<String, Pair<Long, Long>>>> createPermutationsOfAvailableQuantitiesOfIngredient(Map<ProductDto, Map<String, Pair<Long, Long>>> productDtoMap,
                                                                                                                 ProductDto ingredient) {
        LOGGER.debug("In createPermutationsOfAvailableQuantitiesOfIngredient private method");

        List<Pair<String, Pair<Long, Long>>> quantitiesPerParent = productDtoMap.get(ingredient).entrySet()
                .stream()
                .map(set -> new Pair<>(set.getKey(), new Pair<>(set.getValue().getFirst(), set.getValue().getSecond())))
                .collect(Collectors.toList());

        return Collections2.permutations(quantitiesPerParent);
    }

    private ProductPriceComponentsSet createCompositeProductPriceComponentsSet(ProductDto tempProduct, BigDecimal subPrice,
                                                                               Map<String, List<Integer>> componentIngredientsNamesMap,
                                                                               Map<Integer, ProductPriceComponentsSet> productPriceComponentsSetByProductIdMap) {
        LOGGER.debug("In createCompositeProductPriceComponentsSet private method");

        ProductPriceComponentsSet product = new ProductPriceComponentsSet();

        product.setPrice(subPrice);
        product.setComponentIngredients(componentIngredientsNamesMap);
        product.setProductName(tempProduct.getItemName());

        productPriceComponentsSetByProductIdMap.put(product.getProductId(), product);
        return product;
    }

    private ProductPriceComponentsSet createNotCompositeProductPriceComponentsSet(String ingredientName,
                                                                                  Pair<String, Pair<Long, Long>> permutationsOfQuantity,
                                                                                  BigDecimal productPrice,
                                                                                  long predecessorsQuantitySum,
                                                                                  Map<Integer, ProductPriceComponentsSet> productPriceComponentsSetByProductIdMap) {
        LOGGER.debug("In createNotCompositeProductPriceComponentsSet private method");

        ProductPriceComponentsSet ingredientPriceVariant = new ProductPriceComponentsSet();

        ingredientPriceVariant.setPrice(productPrice);
        ingredientPriceVariant.getReservedQuantityRangeStartEnd().setFirst(predecessorsQuantitySum);
        ingredientPriceVariant.getReservedQuantityRangeStartEnd().setSecond(predecessorsQuantitySum + permutationsOfQuantity.getSecond().getSecond());
        ingredientPriceVariant.setProductName(ingredientName);

        productPriceComponentsSetByProductIdMap.put(ingredientPriceVariant.getProductId(), ingredientPriceVariant);
        return ingredientPriceVariant;
    }

    private boolean isConflictWhenOverlappingRangeOfSameIngredientsQuantities(Map<ProductPriceComponentsSet, ProductPriceComponentsSet> sameIngredientsMap) {
        LOGGER.debug("In isConflictWhenOverlappingRangeOfSameIngredientsQuantities private method");

        if (!sameIngredientsMap.isEmpty()) {
            for (ProductPriceComponentsSet ingredientCandidate : sameIngredientsMap.keySet()) {
                if (ingredientCandidate.getComponentIngredients().isEmpty() &&
                        isQuantityRangesAreOverlapping(ingredientCandidate,
                                sameIngredientsMap.get(ingredientCandidate))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isQuantityRangesAreOverlapping(ProductPriceComponentsSet ingredientPriceVariant,
                                                   ProductPriceComponentsSet ingredient) {
        LOGGER.debug("In isQuantityRangesAreOverlapping private method");

        long existingIngredientRangeStart = ingredient.getReservedQuantityRangeStartEnd().getFirst();
        long existingIngredientRangeEnd = ingredient.getReservedQuantityRangeStartEnd().getSecond();
        long candidateIngredientRangeStart = ingredientPriceVariant.getReservedQuantityRangeStartEnd().getFirst();
        long candidateIngredientRangeEnd = ingredientPriceVariant.getReservedQuantityRangeStartEnd().getSecond();

        return (existingIngredientRangeStart >= candidateIngredientRangeStart &&
                existingIngredientRangeStart < candidateIngredientRangeEnd) ||
                (existingIngredientRangeEnd > candidateIngredientRangeStart &&
                        existingIngredientRangeStart <= candidateIngredientRangeStart);

    }

    private Map<ProductPriceComponentsSet, ProductPriceComponentsSet> getMapOfExistingIngredientsSameAsCandidateOrContainedFromCandidate(List<ProductPriceComponentsSet> compositeProductPriceVariant,
                                                                                                                                         ProductPriceComponentsSet ingredientPriceVariant,
                                                                                                                                         Map<Integer, ProductPriceComponentsSet> productPriceComponentsSetByProductIdMap) {
        LOGGER.debug("In getMapOfExistingIngredientsSameAsCandidateOrContainedFromCandidate private method");

        if (ingredientPriceVariant.getComponentIngredients().isEmpty()) {
            return populateResultWhenCandidateAndExistingProductAreSimple(compositeProductPriceVariant,
                    ingredientPriceVariant, productPriceComponentsSetByProductIdMap);
        }
        Map<ProductPriceComponentsSet, ProductPriceComponentsSet> result = new HashMap<>();

        populateResultWhenCandidateIsCompositeAndExistingProductIsSimple(compositeProductPriceVariant,
                ingredientPriceVariant, productPriceComponentsSetByProductIdMap, result);

        populateResultWhenCandidateAndExistingProductAreComposite(compositeProductPriceVariant,
                ingredientPriceVariant, productPriceComponentsSetByProductIdMap, result);

        return result;
    }

    private Map<ProductPriceComponentsSet, ProductPriceComponentsSet> populateResultWhenCandidateAndExistingProductAreSimple(List<ProductPriceComponentsSet> compositeProductPriceVariant,
                                                                                                                             ProductPriceComponentsSet ingredientPriceVariant,
                                                                                                                             Map<Integer, ProductPriceComponentsSet> productPriceComponentsSetByProductIdMap) {
        LOGGER.debug("In populateResultWhenCandidateAndExistingProductAreSimple private method");

        return compositeProductPriceVariant.stream()
                .filter(priceComponentsSet -> priceComponentsSet.getComponentIngredients().containsKey(ingredientPriceVariant.getProductName()))
                .flatMap(priceComponentsSet -> priceComponentsSet.getComponentIngredients().get(ingredientPriceVariant.getProductName()).stream())
                .map(productPriceComponentsSetByProductIdMap::get)
                .collect(Collectors.toMap(k -> ingredientPriceVariant, v -> v));
    }

    private void populateResultWhenCandidateAndExistingProductAreComposite(List<ProductPriceComponentsSet> compositeProductPriceVariant,
                                                                           ProductPriceComponentsSet ingredientPriceVariant,
                                                                           Map<Integer, ProductPriceComponentsSet> productPriceComponentsSetByProductIdMap,
                                                                           Map<ProductPriceComponentsSet, ProductPriceComponentsSet> result) {
        LOGGER.debug("In populateResultWhenCandidateAndExistingProductAreComposite private method");

        for (ProductPriceComponentsSet productPriceComponentsSet : compositeProductPriceVariant) {
            if (productPriceComponentsSet.getComponentIngredients().isEmpty() ||
                    productPriceComponentsSet.getProductName().equals(ingredientPriceVariant.getProductName())) {
                continue;
            }
            for (Map.Entry<String, List<Integer>> entry : productPriceComponentsSet.getComponentIngredients().entrySet()) {
                if (!ingredientPriceVariant.getComponentIngredients().containsKey(entry.getKey())) {
                    continue;
                }
                for (int candidateProductId : ingredientPriceVariant.getComponentIngredients().get(entry.getKey())) {
                    ProductPriceComponentsSet candidate = productPriceComponentsSetByProductIdMap.get(candidateProductId);
                    for (int existingProductId : entry.getValue()) {
                        ProductPriceComponentsSet existingProduct = productPriceComponentsSetByProductIdMap.get(existingProductId);
                        result.put(candidate, existingProduct);
                    }

                }
            }
        }
    }

    private void populateResultWhenCandidateIsCompositeAndExistingProductIsSimple(List<ProductPriceComponentsSet> compositeProductPriceVariant,
                                                                                  ProductPriceComponentsSet ingredientPriceVariant,
                                                                                  Map<Integer, ProductPriceComponentsSet> productPriceComponentsSetByProductIdMap,
                                                                                  Map<ProductPriceComponentsSet, ProductPriceComponentsSet> result) {
        LOGGER.debug("In populateResultWhenCandidateIsCompositeAndExistingProductIsSimple private method");

        for (ProductPriceComponentsSet productPriceComponentsSet : compositeProductPriceVariant) {
            if (!ingredientPriceVariant.getComponentIngredients().containsKey(productPriceComponentsSet.getProductName())) {
                continue;
            }
            for (int ingredientProductId : ingredientPriceVariant.getComponentIngredients().get(productPriceComponentsSet.getProductName())) {
                ProductPriceComponentsSet candidate = productPriceComponentsSetByProductIdMap.get(ingredientProductId);
                result.put(candidate, productPriceComponentsSet);
            }
        }
    }

    private List<ProductSpecification> getDeepCopyOfProductSpecifications(List<ProductSpecification> productSpecificationList) {
        LOGGER.debug("In getDeepCopyOfProductSpecifications private method");

        Gson gson = new Gson();
        String productSpecificationListAsJson = gson.toJson(productSpecificationList);
        Type productSpecificationListType = new TypeToken<ArrayList<ProductSpecification>>() {
        }.getType();

        return gson.fromJson(productSpecificationListAsJson, productSpecificationListType);
    }

    private Optional<ProductDto> getProductWhichIngredientsAreNotCompositeOrIsChecked(Map<String, ProductDto> productDtoNamesMap,
                                                                                      List<ProductDto> productDtoList) {
        LOGGER.debug("In getProductWhichIngredientsAreNotCompositeOrIsChecked private method");

        return productDtoList.stream()
                .filter(productDto -> isLeafIngredient(productDtoNamesMap, productDto) ^
                        productDto.getTotalPrice().equals(BigDecimal.valueOf(-1)))
                .findFirst();
    }

    private boolean isLeafIngredient(Map<String, ProductDto> productDtoNamesMap, ProductDto parentProduct) {
        LOGGER.debug("In isLeafIngredient private method");

        return parentProduct.getIngredients().keySet().stream()
                .allMatch(productName -> {
                    ProductDto productDto = productDtoNamesMap.get(productName);
                    return productDto.getIngredients().isEmpty() ^ productDto.getTotalPrice().equals(BigDecimal.valueOf(-1));
                });
    }

    private void populateConstituentProductsDataToDataStructures(String clientId,
                                                                 Map<Product, Map<String, Pair<Long, Long>>> products,
                                                                 Map<ProductDto, Map<String, Pair<Long, Long>>> productDtoMap,
                                                                 Map<String, List<ProductSpecification>> productSpecificationMap) {
        LOGGER.debug("In populateConstituentProductsDataToDataStructures private method");

        for (Product product : products.keySet()) {
            long tempQuantity = products.get(product).values()
                    .stream()
                    .mapToLong(Pair::getSecond)
                    .sum();

            getProductsMarketDataFromOrderBook(clientId, product, tempQuantity,
                    productSpecificationMap);

            convertProductToProductDto(product, products, productDtoMap);
        }
    }

    private void populateOrderedProductDataToDataStructures(String clientId, String itemName, long quantity,
                                                            Map<Product, Map<String, Pair<Long, Long>>> products,
                                                            Map<ProductDto, Map<String, Pair<Long, Long>>> productDtoMap,
                                                            Map<String, List<ProductSpecification>> productSpecificationMap) {
        LOGGER.debug("In populateOrderedProductDataToDataStructures private method");

        Product orderedProduct = productService.getProductFromDatabase(itemName);
        products.put(orderedProduct, new LinkedHashMap<String, Pair<Long, Long>>() {{
            put(itemName, new Pair<>(1L, quantity));
        }});

        getProductsMarketDataFromOrderBook(clientId, orderedProduct, quantity,
                productSpecificationMap);
        convertProductToProductDto(orderedProduct, products, productDtoMap);
    }


    private void convertProductToProductDto(Product product, Map<Product, Map<String, Pair<Long, Long>>> products,
                                            Map<ProductDto, Map<String, Pair<Long, Long>>> productDtoMap) {
        LOGGER.debug("In convertProductToProductDto private method");

        ProductDto productDto = new ProductDto();
        productDto.setItemName(product.getProductName());
        if (!product.getIngredients().isEmpty()) {
            productDto.setIngredients(product.getIngredients());
        }
        productDtoMap.put(productDto, products.get(product));
    }

    private void reserveProductQuantities(long orderedProductQuantity,
                                          List<ProductSpecification> productSpecificationList) {
        LOGGER.debug("In reserveProductQuantities private method");

        for (ProductSpecification productSpecification : productSpecificationList) {
            if (productSpecification.getQuantity() == 0) {
                continue;
            }
            long tempQuantity = productSpecification.getQuantity();
            if (tempQuantity < orderedProductQuantity) {
                productSpecification.setQuantity(0L);
                orderedProductQuantity -= tempQuantity;
            } else {
                productSpecification.setQuantity(tempQuantity - orderedProductQuantity);
                break;
            }
        }
    }

    private BigDecimal checkPriceForProduct(final Pair<Long, Long> parentAndTotalQuantity,
                                            final List<ProductSpecification> productQuantitiesPerSpecification) {
        LOGGER.debug("In checkPriceForProduct private method");

        BigDecimal result = BigDecimal.ZERO;
        long parentQuantity = parentAndTotalQuantity.getFirst();
        long totalQuantity = parentAndTotalQuantity.getSecond();
        BigDecimal coefficientTotalToParentQuantity = BigDecimal.ONE;

        if (parentQuantity != 1) {
            coefficientTotalToParentQuantity = BigDecimal.valueOf(totalQuantity)
                    .divide(BigDecimal.valueOf(parentQuantity), RoundingMode.HALF_DOWN);
        }
        for (ProductSpecification productSpecification : productQuantitiesPerSpecification) {
            if (productSpecification.getPrice().equals(BigDecimal.valueOf(0.0))) {
                continue;
            }
            long productSpecificationQuantity = productSpecification.getQuantity();
            if (productSpecificationQuantity < totalQuantity) {

                totalQuantity -= productSpecification.getQuantity();

                BigDecimal unitQuantity = BigDecimal.valueOf(productSpecificationQuantity)
                        .divide(coefficientTotalToParentQuantity, RoundingMode.HALF_DOWN);
                result = result.add(unitQuantity.multiply(productSpecification.getPrice()));
            } else {
                BigDecimal unitQuantity = BigDecimal.valueOf(totalQuantity)
                        .divide(coefficientTotalToParentQuantity, RoundingMode.HALF_DOWN);
                result = result.add(unitQuantity
                        .multiply(productSpecification.getPrice()));

                totalQuantity = 0;
                break;
            }
        }
        if (totalQuantity > 0) {
            result = BigDecimal.valueOf(0.0);
        }
        return result;
    }

    private void getProductsMarketDataFromOrderBook(String clientId, Product product, long quantity,
                                                    Map<String, List<ProductSpecification>> productSpecificationMap) {
        LOGGER.debug("In getProductsMarketDataFromOrderBook private method");

        ItemOrderBookResponse orderBookResponse = auroraService.getIngredient(product.getProductName(),
                clientId, quantity);
        List<ProductSpecification> productSpecifications = new ArrayList<>();

        if (orderBookResponse.getOrderbookLayersList().isEmpty()
                && !orderBookResponse.getItemName().equals(product.getProductName())) {
            return;
        }

        String productName = orderBookResponse.getItemName();

        long checkQuantity = quantity;
        for (OrderBookLayer layer : orderBookResponse.getOrderbookLayersList()) {

            ProductSpecification productSpecification = createProductSpecification(layer.getPrice(),
                    layer.getQuantity(), layer.getOrigin().toString());
            checkQuantity -= productSpecification.getQuantity();
            productSpecifications.add(productSpecification);
        }

        if (checkQuantity > 0) {
            productSpecifications.add(createProductSpecification(0.0, checkQuantity, ""));
        }

        productSpecificationMap.put(productName, productSpecifications);
    }

    private ProductSpecification createProductSpecification(double price, long quantity, String origin) {
        LOGGER.debug("In createProductSpecification private method");

        ProductSpecification productSpecification = new ProductSpecification();

        productSpecification.setPrice(BigDecimal.valueOf(price));
        productSpecification.setQuantity(quantity);
        productSpecification.setLocation(origin);

        return productSpecification;
    }
}
