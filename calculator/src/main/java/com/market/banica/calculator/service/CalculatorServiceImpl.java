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
import java.util.Set;


@Service
@RequiredArgsConstructor
public class CalculatorServiceImpl implements CalculatorService {


    private final AuroraClientSideService auroraService;
    private final ProductService productService;
    private final TestData testData;

    @Override
    public List<ProductDto> getRecipe(String clientId, String itemName, int quantity) {

        Set<Product> products = productService.getProductAsListProduct(itemName);

        Map<String, ProductDto> productDtoMap = new HashMap<>();

        for (Product product : products) {
            fillProductSpecificationMapWithData(clientId,productDtoMap,product,quantity);
        }
//        populateProductDtoMapWithData(clientId, products, productDtoMap,
//                parentProduct, quantity);

        List<ProductDto> result = new ArrayList<>();

        Map<String, ProductDto> compositeProductsDtoMap = new HashMap<>();


        for (Map.Entry<String, ProductDto> entry : productDtoMap.entrySet()) {

            if (!entry.getValue().getIngredients().isEmpty()) {

                compositeProductsDtoMap.put(entry.getKey(), entry.getValue());
            }
        }
        Map<String, ProductDto> compositeProductsDtoVerifyParentMap = new HashMap<>(compositeProductsDtoMap);

        for (int i = 0; i < compositeProductsDtoMap.size(); i++) {

            ProductDto tempProduct = compositeProductsDtoMap.values().stream()
                    .filter(k -> {
                        boolean hasCompositeIngredients = false;
                        for (String ingredientName : k.getIngredients().keySet()) {
                            hasCompositeIngredients = !compositeProductsDtoMap.containsKey(ingredientName);
                        }
                        return hasCompositeIngredients;
                    })
                    .findFirst()
                    .orElseThrow(IllegalArgumentException::new);

            long orderedProductQuantity = 0;
            BigDecimal productPrice = BigDecimal.ZERO;
            BigDecimal ingredientsPrice = BigDecimal.ZERO;

            while (compositeProductsDtoVerifyParentMap.keySet().iterator().hasNext()) {

                ProductDto productDto = compositeProductsDtoVerifyParentMap.values().iterator().next();

                if (productDto.getIngredients().containsKey(tempProduct.getItemName())) {

                    orderedProductQuantity = productDto.getIngredients().get(tempProduct.getItemName());
                    compositeProductsDtoVerifyParentMap.get(productDto.getItemName()).getIngredients().remove(tempProduct.getItemName());
                    break;
                }
            }

            productPrice = calculatePriceForProduct(tempProduct,
                    orderedProductQuantity, productPrice);

            for (String productDtoName : tempProduct.getIngredients().keySet()) {

                ProductDto currentProductDto = productDtoMap.get(productDtoName);
                orderedProductQuantity = tempProduct.getIngredients().get(productDtoName);
                ingredientsPrice = calculatePriceForProduct(currentProductDto,
                        orderedProductQuantity, ingredientsPrice);

            }

            if (productPrice.compareTo(ingredientsPrice) > 0) {

                tempProduct.setTotalPrice(ingredientsPrice);
                for (String ingredientName : tempProduct.getIngredients().keySet()) {
                    ProductDto ingredient = productDtoMap.get(ingredientName);
                    if (!result.contains(ingredient)) {
                        result.add(ingredient);
                    }
                }

            } else {

                tempProduct.setTotalPrice(productPrice);
                tempProduct.getIngredients().clear();
            }

            result.add(tempProduct);
        }

        return result;
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

    private void populateProductDtoMapWithData(String clientId, List<Product> products, Map<String, ProductDto> productDtoMap,
                                               Product parentProduct, int quantity) {

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

    private void fillProductSpecificationMapWithData(String clientId, Map<String, ProductDto> productDtoMap, Product product, int quantity) {

        ItemOrderBookResponse orderBookResponse = testData.getTestData().get(product.getProductName());

        String productName = orderBookResponse.getItemName();

        List<ProductSpecification> productSpecifications = new ArrayList<>();
        for (OrderBookLayer layer : orderBookResponse.getOrderbookLayersList()) {
            ProductSpecification productSpecification = createProductSpecification(layer);
            productSpecifications.add(productSpecification);
        }

        ProductDto productDto = new ProductDto();
        productDto.setItemName(productName);
        productDto.setProductSpecifications(productSpecifications);

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
