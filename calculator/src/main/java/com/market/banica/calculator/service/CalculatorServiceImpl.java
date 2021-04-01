package com.market.banica.calculator.service;

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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
@RequiredArgsConstructor
public class CalculatorServiceImpl implements CalculatorService {


    private final AuroraClientSideService auroraService;
    private final ProductService productService;

    @Override
    public ProductDto getRecipe(String clientId, String itemName, int quantity) {

        List<Product> products = productService.getProductAsListProduct(itemName);

        Map<ProductDto, Map<String, Long>> productDtoMap = new HashMap<>();

        Product parentProduct = getProduct(products, itemName);

        populateProductDtoMapWithData(clientId, products, productDtoMap,
                parentProduct, quantity);

        Map<ProductDto, Map<String, Long>> compositeProductsDtoMap = new HashMap<>();
        for (Map.Entry<ProductDto, Map<String, Long>> entry : productDtoMap.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                compositeProductsDtoMap.put(entry.getKey(), entry.getValue());
            }
        }
        for (ProductDto tempProduct : productDtoMap.keySet()) {
            boolean isCompositeProductSet = false;

            while (!isCompositeProductSet) {

                if (tempProduct.getTotalPrice() == null) {

                    long orderedProductQuantity = 0;
                    BigDecimal productPrice = BigDecimal.ZERO;
                    BigDecimal ingredientsPrice = BigDecimal.ZERO;

                    while (compositeProductsDtoMap.keySet().iterator().hasNext()) {

                        ProductDto productDto = compositeProductsDtoMap.keySet().iterator().next();

                        if (productDtoMap.get(productDto).containsKey(tempProduct.getItemName())) {

                            orderedProductQuantity = productDtoMap.get(productDto).get(tempProduct.getItemName());
                            productDtoMap.get(productDto).remove(tempProduct.getItemName());
                            break;
                        }
                    }

                    productPrice = calculatePriceForProduct(tempProduct,
                            orderedProductQuantity, productPrice);

                    List<ProductDto> tempListIngredients = new ArrayList<>();
                    for (String productDtoName : compositeProductsDtoMap.get(tempProduct).keySet()) {

                        ProductDto currentProductDto = getProductDto(productDtoMap, productDtoName);
                        orderedProductQuantity = compositeProductsDtoMap.get(tempProduct).get(productDtoName);
                        ingredientsPrice = calculatePriceForProduct(currentProductDto,
                                orderedProductQuantity, ingredientsPrice);
                        tempListIngredients.add(currentProductDto);

                    }

                    if (productPrice.compareTo(ingredientsPrice) > 0) {

                        tempProduct.setTotalPrice(ingredientsPrice);
                        tempProduct.setIngredients(tempListIngredients);

                    } else {

                        tempProduct.setTotalPrice(productPrice);
                    }

                    isCompositeProductSet = true;
                }
            }
        }
        return getProductDto(compositeProductsDtoMap, itemName);
    }

    private ProductDto getProductDto(Map<ProductDto, Map<String, Long>> productDtoMap, String productDtoName) {
        return productDtoMap.keySet()
                .stream()
                .filter(k -> k.getItemName().equals(productDtoName))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }

    private Product getProduct(List<Product> products, String tempRecipeName) {

        return products.stream()
                .filter(k -> k.getProductName().equals(tempRecipeName))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }

    private BigDecimal calculatePriceForProduct(final ProductDto tempProduct, final long orderedProductQuantity,
                                                final BigDecimal productPrice) {
        BigDecimal result = productPrice;
        long productQuantity = orderedProductQuantity;

        for (ProductSpecification productSpecification : tempProduct.getProductSpecifications()) {
            long tempQuantity = productSpecification.getQuantity();
            if (tempQuantity < productQuantity) {
                productSpecification.setQuantity(0L);
                productQuantity -= tempQuantity;
                result = result.add(BigDecimal.valueOf(tempQuantity)
                        .multiply(productSpecification.getPrice()));
            } else {
                productSpecification.setQuantity(tempQuantity - productQuantity);
                result = result.add(BigDecimal.valueOf(productQuantity)
                        .multiply(productSpecification.getPrice()));
                break;
            }
        }
        return result;
    }

    private void populateProductDtoMapWithData(String clientId, List<Product> products, Map<ProductDto,
            Map<String, Long>> productDtoMap, Product parentProduct, int quantity) {

        fillProductSpecificationMapWithData(clientId, productDtoMap, parentProduct, quantity);

        if (!parentProduct.getIngredients().isEmpty()) {

            parentProduct.getIngredients()
                    .keySet()
                    .stream()
                    .map(ingredientName -> products
                            .stream()
                            .filter(prod -> prod.getProductName().equals(ingredientName))
                            .findFirst()
                            .orElseThrow(IllegalArgumentException::new))
                    .forEach(ingredient -> populateProductDtoMapWithData(clientId, products,
                            productDtoMap, ingredient, quantity));

        }
    }

    private void fillProductSpecificationMapWithData(String clientId, Map<ProductDto,
            Map<String, Long>> productDtoMap, Product product, int quantity) {

        ItemOrderBookResponse orderBookResponse = auroraService.getIngredient(product.getProductName(),
                clientId, quantity);

        String productName = orderBookResponse.getItemName();

        List<ProductSpecification> productSpecifications = new ArrayList<>();
        for (OrderBookLayer layer : orderBookResponse.getOrderbookLayersList()) {
            ProductSpecification productSpecification = createProductSpecification(layer);
            productSpecifications.add(productSpecification);
        }

        ProductDto productDto = new ProductDto();
        productDto.setItemName(productName);
        productDto.setProductSpecifications(productSpecifications);

        Map<String, Long> ingredientQuantitiesMap = new HashMap<>();
        if (product.getIngredients().isEmpty()) {
            productDto.setTotalPrice(BigDecimal.ZERO);
        } else {
            ingredientQuantitiesMap = product.getIngredients();
        }
        productDtoMap.put(productDto, ingredientQuantitiesMap);
    }

    private ProductSpecification createProductSpecification(OrderBookLayer layer) {

        ProductSpecification productSpecification = new ProductSpecification();
        productSpecification.setPrice(BigDecimal.valueOf(layer.getPrice()));
        productSpecification.setQuantity(layer.getQuantity());
        productSpecification.setLocation(layer.getOrigin().toString());

        return productSpecification;
    }
}
