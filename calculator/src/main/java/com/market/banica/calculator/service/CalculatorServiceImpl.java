package com.market.banica.calculator.service;

import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.market.banica.calculator.dto.ProductDto;
import com.market.banica.calculator.dto.ProductPriceComponentsSet;
import com.market.banica.calculator.dto.ProductSpecification;
import com.market.banica.calculator.exception.exceptions.BadResponseException;
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
    Map<String, ProductSpecification> testProductSpecificationMap = new HashMap<>();

    @Override
    public List<ProductDto> getProduct(String clientId, String itemName, long quantity) {

        Map<Product, Map<String, Long>> products = productService.getProductIngredientsWithQuantityPerParent(itemName);

        Map<ProductDto, Map<String, Long>> productDtoMap = new HashMap<>();

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
                                              Map<Product, Map<String, Long>> products,
                                              Map<ProductDto, Map<String, Long>> productDtoMap,
                                              Map<String, List<ProductSpecification>> productSpecificationMap) {

        populateOrderedProductDataToDataStructures(clientId, itemName, quantity,
                products, productDtoMap, productSpecificationMap);

        populateConstituentProductsDataToDataStructures(clientId, products,
                productDtoMap, productSpecificationMap);
    }

    private List<ProductDto> writeProductsFromBestPricePath(Map<String, List<ProductSpecification>> productSpecificationMap,
                                                            Map<String, ProductDto> productDtoNamesMap,
                                                            ProductPriceComponentsSet resultProductPriceComponentsSet,
                                                            Map<Integer, ProductPriceComponentsSet> productPriceComponentsSetByProductIdMap) {

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

        ProductDto newParentProduct = new ProductDto();

        newParentProduct.setItemName(resultProductPriceComponentsSet.getProductName());
        newParentProduct.setTotalPrice(resultProductPriceComponentsSet.getPrice());
        newParentProduct.setIngredients(parentProduct.getIngredients());

        return newParentProduct;
    }

    private ProductDto createSimpleParentProduct(Map<String, List<ProductSpecification>> productSpecificationMap,
                                                 ProductPriceComponentsSet resultProductPriceComponentsSet) {

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

        long start = productPriceComponentsSet.getReservedQuantityRangeStartEnd().getFirst();
        long range = productPriceComponentsSet.getReservedQuantityRangeStartEnd().getSecond() -
                productPriceComponentsSet.getReservedQuantityRangeStartEnd().getFirst();

        for (ProductSpecification productSpecification : productSpecificationMap.get(tempProductName)) {
            // TODO: make buy requests to market from here
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

    private ProductPriceComponentsSet createBestPricePath(Map<ProductDto, Map<String, Long>> productDtoMap,
                                                          Map<String, List<ProductSpecification>> productSpecificationMap,
                                                          Map<String, ProductDto> productDtoNamesMap,
                                                          Map<Integer, ProductPriceComponentsSet> productPriceComponentsSetByProductIdMap,
                                                          String itemName) {

        List<ProductDto> compositeProductDtoList = createListWithOnlyCompositeProducts(productDtoMap);

        Map<String, List<List<ProductPriceComponentsSet>>> result = new HashMap<>();

        createPriceVariantsForProducts(productDtoMap, productSpecificationMap,
                productDtoNamesMap, productPriceComponentsSetByProductIdMap,
                compositeProductDtoList, result);

        ProductDto orderedProduct = productDtoNamesMap.get(itemName);
        long orderedQuantity = productDtoMap.get(orderedProduct).get(itemName);

        List<ProductPriceComponentsSet> orderedProductPriceVariantsSet =
                createPriceVariantsForOrderedProduct(productDtoMap, productSpecificationMap,
                        productPriceComponentsSetByProductIdMap, result,
                        orderedProduct, orderedQuantity);

        Set<ProductPriceComponentsSet> resultSet = new TreeSet<>(orderedProductPriceVariantsSet);

        if (resultSet.isEmpty()) {
            throw new BadResponseException("Out of resources for this product");
        }

        return resultSet.iterator().next();
    }

    private void createPriceVariantsForProducts(Map<ProductDto, Map<String, Long>> productDtoMap,
                                                Map<String, List<ProductSpecification>> productSpecificationMap,
                                                Map<String, ProductDto> productDtoNamesMap,
                                                Map<Integer, ProductPriceComponentsSet> productPriceComponentsSetByProductIdMap,
                                                List<ProductDto> compositeProductDtoList,
                                                Map<String, List<List<ProductPriceComponentsSet>>> result) {

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
        tempProduct.setTotalPrice(BigDecimal.valueOf(-1));
    }

    private List<List<ProductPriceComponentsSet>> createCompositeProductPriceVariantsFromIngredientsPriceVariants(Map<Integer, ProductPriceComponentsSet> productPriceComponentsSetByProductIdMap,
                                                                                                                  ProductDto tempProduct,
                                                                                                                  List<List<ProductPriceComponentsSet>> cartesianProductOfIngredientsPriceVariants) {
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

    private List<List<ProductPriceComponentsSet>> createPriceVariantsForIngredients(Map<ProductDto, Map<String, Long>> productDtoMap,
                                                                                    Map<String, List<ProductSpecification>> productSpecificationMap,
                                                                                    Map<String, ProductDto> productDtoNamesMap,
                                                                                    Map<Integer, ProductPriceComponentsSet> productPriceComponentsSetByProductIdMap,
                                                                                    Map<String, List<List<ProductPriceComponentsSet>>> result,
                                                                                    ProductDto tempProduct) {


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

    private List<ProductPriceComponentsSet> createPriceVariantsForOrderedProduct(Map<ProductDto, Map<String, Long>> productDtoMap,
                                                                                 Map<String, List<ProductSpecification>> productSpecificationMap,
                                                                                 Map<Integer, ProductPriceComponentsSet> productPriceComponentsSetByProductIdMap,
                                                                                 Map<String, List<List<ProductPriceComponentsSet>>> result,
                                                                                 ProductDto orderedProduct, long quantity) {

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

    private List<ProductPriceComponentsSet> createPriceVariantsForCompositeProduct(Map<ProductDto, Map<String, Long>> productDtoMap,
                                                                                   Map<String, List<ProductSpecification>> productSpecificationMap,
                                                                                   Map<Integer, ProductPriceComponentsSet> productPriceComponentsSetByProductIdMap,
                                                                                   Map<String, List<List<ProductPriceComponentsSet>>> result,
                                                                                   ProductDto tempProduct, ProductDto ingredient) {

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

    private List<ProductDto> createListWithOnlyCompositeProducts(Map<ProductDto, Map<String, Long>> productDtoMap) {

        List<ProductDto> compositeProductDtoList = new ArrayList<>();
        for (ProductDto productDto : productDtoMap.keySet()) {

            if (productDto.getIngredients().isEmpty()) {

                continue;
            }
            compositeProductDtoList.add(productDto);
        }
        return compositeProductDtoList;
    }

    private Set<ProductPriceComponentsSet> calculatePossiblePricesForProductFromProductSpecifications(Map<ProductDto, Map<String, Long>> productDtoMap,
                                                                                                      Map<String, List<ProductSpecification>> productSpecificationMap,
                                                                                                      ProductDto parentProduct,
                                                                                                      ProductDto ingredient,
                                                                                                      Map<Integer, ProductPriceComponentsSet> productPriceComponentsSetByProductIdMap) {

        Collection<List<Pair<String, Long>>> permutationsOfQuantitiesOfProductSet =
                createPermutationsOfAvailableQuantitiesOfIngredient(productDtoMap, ingredient);

        return createPossibleProductPriceComponentSetsForProduct(
                productSpecificationMap, parentProduct, ingredient.getItemName(),
                permutationsOfQuantitiesOfProductSet,
                productPriceComponentsSetByProductIdMap);
    }

    private Set<ProductPriceComponentsSet> createPossibleProductPriceComponentSetsForProduct(Map<String, List<ProductSpecification>> productSpecificationMap,
                                                                                             ProductDto tempProduct, String ingredientName,
                                                                                             Collection<List<Pair<String, Long>>> permutationsOfQuantitiesOfProductSet,
                                                                                             Map<Integer, ProductPriceComponentsSet> productPriceComponentsSetByProductIdMap) {

        Set<ProductPriceComponentsSet> ingredientPriceVariantsSet = new HashSet<>();
        for (List<Pair<String, Long>> permutationsOfQuantities : permutationsOfQuantitiesOfProductSet) {

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
                                                                              List<Pair<String, Long>> permutationsOfQuantities,
                                                                              List<ProductSpecification> deepCopyOfProductSpecification,
                                                                              Map<Integer, ProductPriceComponentsSet> productPriceComponentsSetByProductIdMap) {

        for (Pair<String, Long> permutationsOfQuantity : permutationsOfQuantities) {

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

            reserveProductQuantities(permutationsOfQuantity.getSecond(),
                    deepCopyOfProductSpecification);
        }
    }

    private long calculatePredecessorsQuantitySum(List<Pair<String, Long>> permutationsOfQuantities,
                                                  Pair<String, Long> permutationsOfQuantity) {

        long predecessorsQuantitySum = 0;
        for (Pair<String, Long> permutationsOfQuantityParent : permutationsOfQuantities) {

            if (permutationsOfQuantityParent.equals(permutationsOfQuantity)) {

                break;
            }
            predecessorsQuantitySum += permutationsOfQuantityParent.getSecond();
        }
        return predecessorsQuantitySum;
    }

    private Collection<List<Pair<String, Long>>> createPermutationsOfAvailableQuantitiesOfIngredient(Map<ProductDto, Map<String, Long>> productDtoMap,
                                                                                                     ProductDto ingredient) {

        List<Pair<String, Long>> quantitiesPerParent = productDtoMap.get(ingredient).entrySet()
                .stream()
                .map(set -> new Pair<>(set.getKey(), set.getValue()))
                .collect(Collectors.toList());

        return Collections2.permutations(quantitiesPerParent);
    }

    private ProductPriceComponentsSet createCompositeProductPriceComponentsSet(ProductDto tempProduct, BigDecimal subPrice,
                                                                               Map<String, List<Integer>> componentIngredientsNamesMap,
                                                                               Map<Integer, ProductPriceComponentsSet> productPriceComponentsSetByProductIdMap) {

        ProductPriceComponentsSet product = new ProductPriceComponentsSet();

        product.setPrice(subPrice);
        product.setComponentIngredients(componentIngredientsNamesMap);
        product.setProductName(tempProduct.getItemName());

        productPriceComponentsSetByProductIdMap.put(product.getProductId(), product);

        return product;
    }

    private ProductPriceComponentsSet createNotCompositeProductPriceComponentsSet(String ingredientName,
                                                                                  Pair<String, Long> permutationsOfQuantity,
                                                                                  BigDecimal productPrice,
                                                                                  long predecessorsQuantitySum,
                                                                                  Map<Integer, ProductPriceComponentsSet> productPriceComponentsSetByProductIdMap) {

        ProductPriceComponentsSet ingredientPriceVariant = new ProductPriceComponentsSet();

        ingredientPriceVariant.setPrice(productPrice);
        ingredientPriceVariant.getReservedQuantityRangeStartEnd().setFirst(predecessorsQuantitySum);
        ingredientPriceVariant.getReservedQuantityRangeStartEnd().setSecond(predecessorsQuantitySum + permutationsOfQuantity.getSecond());
        ingredientPriceVariant.setProductName(ingredientName);

        productPriceComponentsSetByProductIdMap.put(ingredientPriceVariant.getProductId(), ingredientPriceVariant);

        return ingredientPriceVariant;
    }

    private boolean isConflictWhenOverlappingRangeOfSameIngredientsQuantities(Map<ProductPriceComponentsSet, ProductPriceComponentsSet> sameIngredientsMap) {

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

        Gson gson = new Gson();
        String productSpecificationListAsJson = gson.toJson(productSpecificationList);
        Type productSpecificationListType = new TypeToken<ArrayList<ProductSpecification>>() {
        }.getType();

        return gson.fromJson(productSpecificationListAsJson, productSpecificationListType);
    }

    private Optional<ProductDto> getProductWhichIngredientsAreNotCompositeOrIsChecked(Map<String, ProductDto> productDtoNamesMap,
                                                                                      List<ProductDto> productDtoList) {

        return productDtoList.stream()
                .filter(productDto -> isLeafIngredient(productDtoNamesMap, productDto) ^
                        productDto.getTotalPrice().equals(BigDecimal.valueOf(-1)))
                .findFirst();
    }

    private boolean isLeafIngredient(Map<String, ProductDto> productDtoNamesMap, ProductDto parentProduct) {

        return parentProduct.getIngredients().keySet().stream()
                .allMatch(productName -> {
                    ProductDto productDto = productDtoNamesMap.get(productName);
                    return productDto.getIngredients().isEmpty() ^ productDto.getTotalPrice().equals(BigDecimal.valueOf(-1));
                });
    }

    private void populateConstituentProductsDataToDataStructures(String clientId, Map<Product, Map<String, Long>> products,
                                                                 Map<ProductDto, Map<String, Long>> productDtoMap,
                                                                 Map<String, List<ProductSpecification>> productSpecificationMap) {

        for (Product product : products.keySet()) {

            long tempQuantity = products.get(product).values().stream().mapToLong(Long::longValue).sum();

            getProductsMarketDataFromOrderBook(clientId, product, tempQuantity,
                    productSpecificationMap);

            convertProductToProductDto(product, products, productDtoMap);
        }
    }

    private void populateOrderedProductDataToDataStructures(String clientId, String itemName, long quantity,
                                                            Map<Product, Map<String, Long>> products,
                                                            Map<ProductDto, Map<String, Long>> productDtoMap,
                                                            Map<String, List<ProductSpecification>> productSpecificationMap) {

        Product orderedProduct = productService.getProductFromDatabase(itemName);

        products.put(orderedProduct, new HashMap<String, Long>() {{
            put(itemName, quantity);
        }});

        getProductsMarketDataFromOrderBook(clientId, orderedProduct, quantity,
                productSpecificationMap);
        convertProductToProductDto(orderedProduct, products, productDtoMap);
    }


    private void convertProductToProductDto(Product product, Map<Product, Map<String, Long>> products,
                                            Map<ProductDto, Map<String, Long>> productDtoMap) {

        ProductDto productDto = new ProductDto();

        productDto.setItemName(product.getProductName());

        if (!product.getIngredients().isEmpty()) {

            productDto.setIngredients(product.getIngredients());
        }

        productDtoMap.put(productDto, products.get(product));
    }

    private void reserveProductQuantities(long orderedProductQuantity,
                                          List<ProductSpecification> productSpecificationList) {

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

    private BigDecimal checkPriceForProduct(long orderedProductQuantity,
                                            final List<ProductSpecification> productQuantitiesPerSpecification) {

        BigDecimal result = BigDecimal.ZERO;

        for (ProductSpecification productSpecification : productQuantitiesPerSpecification) {

            if (productSpecification.getPrice().equals(BigDecimal.valueOf(0.0))) {
                continue;
            }
            long productSpecificationQuantity = productSpecification.getQuantity();
            if (productSpecificationQuantity < orderedProductQuantity) {

                orderedProductQuantity -= productSpecification.getQuantity();
                result = result.add(BigDecimal.valueOf(productSpecificationQuantity)
                        .multiply(productSpecification.getPrice()));

            } else {

                result = result.add(BigDecimal.valueOf(orderedProductQuantity)
                        .multiply(productSpecification.getPrice()));

                orderedProductQuantity = 0;
                break;
            }
        }
        if (orderedProductQuantity > 0) {
            result = BigDecimal.valueOf(0.0);
        }
        return result;
    }

    private void getProductsMarketDataFromOrderBook(String clientId, Product product, long quantity,
                                                    Map<String, List<ProductSpecification>> productSpecificationMap) {

        ItemOrderBookResponse orderBookResponse = auroraService.getIngredient(product.getProductName(),
                clientId, quantity);

        List<ProductSpecification> productSpecifications = new ArrayList<>();

        if (orderBookResponse.getOrderbookLayersList().isEmpty()
                && !orderBookResponse.getItemName().equals(product.getProductName())) {
            return;
        }
        LOGGER.info("Response received for product {}", product.getProductName());

        String productName = orderBookResponse.getItemName();

        long checkQuantity = quantity;
        for (OrderBookLayer layer : orderBookResponse.getOrderbookLayersList()) {

            ProductSpecification productSpecification = createProductSpecification(layer.getPrice(),
                    layer.getQuantity(), layer.getOrigin().toString());
            checkQuantity -= productSpecification.getQuantity();
            productSpecifications.add(productSpecification);
            testProductSpecificationMap.put(productName, createProductSpecification(layer.getPrice(), layer.getQuantity(), layer.getOrigin().toString()));
//            auroraService.buyProduct(productName, layer);
        }

        if (checkQuantity > 0) {
            productSpecifications.add(createProductSpecification(0.0, checkQuantity, ""));
        }

        productSpecificationMap.put(productName, productSpecifications);
    }

    private ProductSpecification createProductSpecification(double price, long quantity, String origin) {
        ProductSpecification productSpecification = new ProductSpecification();

        productSpecification.setPrice(BigDecimal.valueOf(price));
        productSpecification.setQuantity(quantity);
        productSpecification.setLocation(origin);

        return productSpecification;
    }
}
