package com.market.banica.calculator.service;

import com.market.AvailabilityResponse;
import com.market.Origin;
import com.market.banica.calculator.dto.ItemDto;
import com.market.banica.calculator.dto.ProductDto;
import com.market.banica.calculator.dto.ProductSpecification;
import com.market.banica.calculator.service.contract.CalculatorService;
import com.market.banica.calculator.service.contract.TransactionService;
import com.market.banica.calculator.service.grpc.AuroraClientSideService;
import com.market.banica.common.exception.ProductNotAvailableException;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class TransactionServiceImpl implements TransactionService {
    private static final String PRODUCT_NOT_AVAILABLE_MESSAGE = "%s with quantity %d is currently not available on the %s market";

    private final CalculatorService calculatorService;
    private final AuroraClientSideService auroraClientSideService;

    @Override
    public List<ProductDto> buyProduct(String clientId, String itemName, long quantity) throws ProductNotAvailableException {
        List<ProductDto> purchaseProducts = this.calculatorService.getProduct(clientId, itemName, quantity);
        List<ProductDto> notCompoundProducts = getNotCompoundProducts(purchaseProducts);
        List<ItemDto> pendingItems = new ArrayList<>();

        for (ProductDto purchaseProduct : notCompoundProducts) {
            String productName = purchaseProduct.getItemName();

            for (ProductSpecification productSpecification : purchaseProduct.getProductSpecifications()) {
                BigDecimal productPrice = productSpecification.getPrice();
                Long productQuantity = productSpecification.getQuantity();
                Origin productOrigin = Origin.valueOf(productSpecification.getLocation().toUpperCase(Locale.ROOT));

                AvailabilityResponse availabilityResponse =
                        this.auroraClientSideService
                                .checkAvailability(productName, productPrice.doubleValue(), productQuantity, productOrigin);

                if (!availabilityResponse.getIsAvailable()) {
                    returnPendingProducts(pendingItems);
                    throw new ProductNotAvailableException(String.format(PRODUCT_NOT_AVAILABLE_MESSAGE,
                            availabilityResponse.getItemName(),
                            availabilityResponse.getItemQuantity(),
                            availabilityResponse.getMarketName()));
                }
                pendingItems.add(new ItemDto(productName, productPrice, productOrigin.toString(), productQuantity));
            }
        }
        buyPendingProducts(pendingItems);
        return purchaseProducts;
    }

    private List<ProductDto> getNotCompoundProducts(List<ProductDto> purchaseProducts) {
        return purchaseProducts
                .stream()
                .filter(productDto -> !productDto.getProductSpecifications().isEmpty())
                .collect(Collectors.toList());
    }

    private void returnPendingProducts(List<ItemDto> pendingItems) {
        pendingItems.forEach(item -> auroraClientSideService.returnPendingProductInMarket(
                item.getName(),
                item.getPrice().doubleValue(),
                item.getQuantity(),
                item.getLocation()));
    }

    private void buyPendingProducts(List<ItemDto> pendingItems) {
        pendingItems.forEach(item -> auroraClientSideService.buyProductFromMarket(
                item.getName(),
                item.getPrice().doubleValue(),
                item.getQuantity(),
                item.getLocation()));
    }
}