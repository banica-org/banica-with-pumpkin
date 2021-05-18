package com.market.banica.calculator.service;

import com.market.AvailabilityResponse;
import com.market.Origin;
import com.market.banica.calculator.dto.ProductDto;
import com.market.banica.calculator.dto.ProductSpecification;
import com.market.banica.calculator.service.contract.CalculatorService;
import com.market.banica.calculator.service.grpc.AuroraClientSideService;
import com.market.banica.common.exception.ProductNotAvailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    public static final String CLIENT_ID = "clienID";
    public static final String ITEM_NAME_BANICA = "banica";
    public static final String ITEM_NAME_EGGS = "eggs";
    public static final String ITEM_NAME_CRUSTS = "crusts";
    public static final String ITEM_ORIGIN = "america";
    public static final long ITEM_QUANTITY = 2L;
    public static final BigDecimal ITEM_PRICE = BigDecimal.valueOf(2);

    @Mock
    private CalculatorService calculatorService;
    @Mock
    private AuroraClientSideService auroraClientSideService;

    private List<ProductDto> purchaseProducts;

    private TransactionServiceImpl transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionServiceImpl(calculatorService, auroraClientSideService);
        purchaseProducts = geneRatePurchaseProducts();
    }

    private List<ProductDto> geneRatePurchaseProducts() {
        ProductSpecification eggsSpecification = generateProductSpecification(ITEM_PRICE, ITEM_ORIGIN, ITEM_QUANTITY);
        ProductDto eggs = generateProductDto(ITEM_NAME_EGGS, ITEM_PRICE, eggsSpecification);
        ProductSpecification crustsSpecification = generateProductSpecification(ITEM_PRICE, ITEM_ORIGIN, ITEM_QUANTITY);
        ProductDto crusts = generateProductDto(ITEM_NAME_CRUSTS, ITEM_PRICE, crustsSpecification);
        return Arrays.asList(eggs, crusts);
    }

    private ProductSpecification generateProductSpecification(BigDecimal price, String location, Long quantity) {
        ProductSpecification productSpecification = new ProductSpecification();
        productSpecification.setPrice(price);
        productSpecification.setLocation(location);
        productSpecification.setQuantity(quantity);
        return productSpecification;
    }

    private ProductDto generateProductDto(String itemName, BigDecimal totalPrice, ProductSpecification productSpecification) {
        ProductDto productDto = new ProductDto();
        productDto.setItemName(itemName);
        productDto.setTotalPrice(totalPrice);
        productDto.setProductSpecifications(Collections.singletonList(productSpecification));
        return productDto;
    }

    @Test
    void buyProductShouldReturnPurchaseProductRecipe() throws ProductNotAvailableException {
        //Arrange
        when(calculatorService.getProduct(
                CLIENT_ID,
                ITEM_NAME_BANICA,
                ITEM_QUANTITY))
                .thenReturn(purchaseProducts);
        AvailabilityResponse eggsAvailabilityResponse = generateAvailabilityResponseForAvailableProduct(
                ITEM_NAME_EGGS,
                ITEM_PRICE.doubleValue(),
                ITEM_QUANTITY,
                Origin.AMERICA);

        when(auroraClientSideService.checkAvailability(ITEM_NAME_EGGS, ITEM_PRICE.doubleValue(), ITEM_QUANTITY, Origin.AMERICA)).thenReturn(eggsAvailabilityResponse);
        AvailabilityResponse crustsAvailabilityResponse = generateAvailabilityResponseForAvailableProduct(
                ITEM_NAME_CRUSTS,
                ITEM_PRICE.doubleValue(),
                ITEM_QUANTITY,
                Origin.AMERICA);
        when(auroraClientSideService.checkAvailability(ITEM_NAME_CRUSTS, ITEM_PRICE.doubleValue(), ITEM_QUANTITY, Origin.AMERICA)).thenReturn(crustsAvailabilityResponse);

        //Act
        List<ProductDto> purchaseProducts = transactionService.buyProduct(CLIENT_ID, ITEM_NAME_BANICA, ITEM_QUANTITY);


        //Assert
        assertEquals(purchaseProducts.get(0).getItemName(), ITEM_NAME_EGGS);
        assertEquals(purchaseProducts.get(1).getItemName(), ITEM_NAME_CRUSTS);

    }

    @Test
    void buyProductShouldReturnPurchaseProductRecipeShouldThrowProductNotFoundException() {
        assertThrows(ProductNotAvailableException.class, () -> {
            //Arrange
            when(calculatorService.getProduct(CLIENT_ID, ITEM_NAME_BANICA, ITEM_QUANTITY)).thenReturn(purchaseProducts);
            AvailabilityResponse eggsAvailabilityResponse = generateAvailabilityResponseForAvailableProduct(
                    ITEM_NAME_EGGS,
                    ITEM_PRICE.doubleValue(),
                    ITEM_QUANTITY,
                    Origin.AMERICA);
            when(auroraClientSideService.checkAvailability(ITEM_NAME_EGGS, ITEM_PRICE.doubleValue(), ITEM_QUANTITY, Origin.AMERICA)).thenReturn(eggsAvailabilityResponse);

            AvailabilityResponse crustsAvailabilityResponse = generateAvailabilityResponseForUnavailableProduct(
                    ITEM_NAME_CRUSTS,
                    ITEM_PRICE.doubleValue(),
                    ITEM_QUANTITY,
                    Origin.AMERICA);
            when(auroraClientSideService.checkAvailability(ITEM_NAME_CRUSTS, ITEM_PRICE.doubleValue(), ITEM_QUANTITY, Origin.AMERICA)).thenReturn(crustsAvailabilityResponse);

            //Act
            transactionService.buyProduct(CLIENT_ID, ITEM_NAME_BANICA, ITEM_QUANTITY);

        });
    }

    @Test
    void buyProductShouldThrowProductNotAvailableException() {
        //Assert
        assertThrows(ProductNotAvailableException.class, () -> {

            //Arrange
            when(calculatorService.getProduct(CLIENT_ID, ITEM_NAME_BANICA, ITEM_QUANTITY)).thenThrow(ProductNotAvailableException.class);

            //Act
            transactionService.buyProduct(CLIENT_ID, ITEM_NAME_BANICA, ITEM_QUANTITY);
        });
    }

    private AvailabilityResponse generateAvailabilityResponseForAvailableProduct(String itemName, double price, long quantity, Origin origin) {
        return AvailabilityResponse.newBuilder()
                .setItemName(itemName)
                .setItemPrice(price)
                .setItemQuantity(quantity)
                .setMarketName(String.valueOf(origin))
                .setIsAvailable(true)
                .build();

    }

    private AvailabilityResponse generateAvailabilityResponseForUnavailableProduct(String itemName, double price, long quantity, Origin origin) {
        return AvailabilityResponse.newBuilder()
                .setIsAvailable(false)
                .setItemName(itemName)
                .setItemPrice(price)
                .setItemQuantity(quantity)
                .setMarketName(String.valueOf(origin))
                .build();

    }
}