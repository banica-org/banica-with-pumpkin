package com.market.banica.calculator.service;

import com.market.AvailabilityResponse;
import com.market.BuySellProductResponse;
import com.market.Origin;
import com.market.banica.calculator.dto.ItemDto;
import com.market.banica.calculator.dto.ProductDto;
import com.market.banica.calculator.dto.ProductSpecification;
import com.market.banica.calculator.service.contract.CalculatorService;
import com.market.banica.calculator.service.contract.TransactionService;
import com.market.banica.calculator.service.grpc.AuroraClientSideService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class TransactionServiceImpl implements TransactionService {

    public static final String AVAILABLE_BUY_RESPONSE_MESSAGE = "Available";
    public static final String UNAVAILABLE_BUY_RESPONSE_MESSAGE = "Unavailable";

    private final CalculatorService calculatorService;
    private final AuroraClientSideService auroraClientSideService;


    @Override
    public List<ProductDto> buyProduct(String clientId, String itemName, long quantity) {
        List<ProductDto> purchaseProducts = getPurchaseProducts(clientId, itemName, quantity);
        List<ProductDto> collect = purchaseProducts.stream().filter(productDto -> !productDto.getProductSpecifications().isEmpty()).collect(Collectors.toList());

        List<ItemDto> pendingItems = new ArrayList<>();

        int productsSpecificationCounter = getProductsSpecificationCounter(collect);
        AtomicInteger successfulBuyResponseCounter = new AtomicInteger();

        boolean areAvailable = true;
        for (int i = 0; i < collect.size() && areAvailable; i++) {
            ProductDto purchaseProduct = collect.get(i);
            String productName = purchaseProduct.getItemName();

            for (ProductSpecification productSpecification : purchaseProduct.getProductSpecifications()) {

                BigDecimal productPrice = productSpecification.getPrice();
                Long productQuantity = productSpecification.getQuantity();
                Origin productOrigin = Origin.valueOf(productSpecification.getLocation());

                AvailabilityResponse buySellProductResponse = auroraClientSideService
                        .checkAvailability(productName, productPrice.doubleValue(), productQuantity, productOrigin);
                if (!buySellProductResponse.getIsAvailable()) {
                    areAvailable = false;
                    break;
                }
                pendingItems.add(new ItemDto(productName, productPrice, productOrigin.toString(), productQuantity, buySellProductResponse.getTimestamp()));
            }
        }
//        for (ProductDto purchaseProduct : collect) {
//
//            String productName = purchaseProduct.getItemName();
//
//            for (ProductSpecification productSpecification : purchaseProduct.getProductSpecifications()) {
//
//                BigDecimal productPrice = productSpecification.getPrice();
//                Long productQuantity = productSpecification.getQuantity();
//                Origin productOrigin = Origin.valueOf(productSpecification.getLocation());
//
//                AvailabilityResponse buySellProductResponse = auroraClientSideService
//                        .checkAvailability(productName, productPrice.doubleValue(), productQuantity, productOrigin);
//                if (!buySellProductResponse.getIsAvailable()) {
//                    areAvailable = false;
//                    break;
//                }
//                pendingItems.add(new ItemDto(productName, productPrice, productOrigin.toString(), productQuantity, buySellProductResponse.getTimestamp()));
//            }
//
//            if (!areAvailable) {
//                break;
//            }
//        }

        if (!areAvailable) {
            returnPendingProducts(pendingItems);
        } else {
            buyPendingProducts(pendingItems);
        }
        return purchaseProducts;

//        if (productsSpecificationCounter == successfulBuyResponseCounter.get()) {
//            return "Congratulations you have buy the product.";
//        }else {
//            return "Unfortunately you can't buy this product.";
//        }

    }

    private void buyPendingProducts(List<ItemDto> pendingItems) {
        pendingItems.forEach(item -> auroraClientSideService.buyProduct(item.getName(), item.getPrice().doubleValue(),
                item.getQuantity(), item.getLocation(), item.getTimeStamp()));
    }

    private void returnPendingProducts(List<ItemDto> pendingItems) {
        pendingItems.forEach(item -> auroraClientSideService.sellProduct(item.getName(), item.getPrice().doubleValue(),
                item.getQuantity(), item.getLocation(), item.getTimeStamp()));
    }

    private int getProductsSpecificationCounter(List<ProductDto> purchaseProducts) {
        return Math.toIntExact(purchaseProducts
                .stream()
                .mapToLong(productDto -> productDto.getProductSpecifications().size())
                .count());

    }

    private List<ProductDto> getPurchaseProducts(String clientId, String itemName, long quantity) {
        return new ArrayList<>(calculatorService.getProduct(clientId, itemName, quantity));
    }
}
