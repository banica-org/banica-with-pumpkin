package com.market.banica.calculator.service.grpc;


import com.aurora.Aurora;
import com.aurora.AuroraServiceGrpc;
import com.google.protobuf.InvalidProtocolBufferException;
import com.market.AvailabilityResponse;
import com.market.Origin;
import com.market.ProductBuyRequest;
import com.market.ProductSellRequest;
import com.market.banica.common.exception.IncorrectResponseException;
import com.orderbook.CancelSubscriptionResponse;
import com.orderbook.InterestsResponse;
import com.orderbook.ItemOrderBookResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class AuroraClientSideService {

    private static final String ORDERBOOK_TOPIC_PREFIX = "orderbook/";



    private static final String CLIENT_ID = "calculator";

    private static final Logger LOGGER = LoggerFactory.getLogger(AuroraClientSideService.class);

    private final AuroraServiceGrpc.AuroraServiceBlockingStub blockingStub;

    @Autowired
    public AuroraClientSideService(AuroraServiceGrpc.AuroraServiceBlockingStub blockingStub) {
        this.blockingStub = blockingStub;
    }

    public void announceInterests(String productName) {
        LOGGER.debug("Inside announceInterests method with parameter product name: {}", productName);

        String message = String.format("%s=subscribe", productName);
        Aurora.AuroraResponse auroraResponse = getAuroraResponse(message);

        if (!auroraResponse.getMessage().is(InterestsResponse.class)) {
            throw new IncorrectResponseException("Incorrect response! Response must be from InterestsResponse type.");
        }
    }

    public void cancelSubscription(String productName) {
        LOGGER.debug("Inside cancelSubscription method with parameter product name: {}", productName);

        String message = String.format("%s=unsubscribe", productName);
        Aurora.AuroraResponse auroraResponse = getAuroraResponse(message);

        if (!auroraResponse.getMessage().is(CancelSubscriptionResponse.class)) {
            throw new IncorrectResponseException("Incorrect response! Response must be from CancelSubscriptionResponse type.");
        }
    }

    public ItemOrderBookResponse getIngredient(String productName, String clientId, long quantity) {
        LOGGER.debug("Inside getIngredient method with parameter product name - {} and client id - {}"
                , productName, clientId);

        String message = String.format("%s/%d", productName, quantity);
        Aurora.AuroraResponse auroraResponse = getAuroraResponse(message);

        if (!auroraResponse.getMessage().is(ItemOrderBookResponse.class)) {
            throw new IncorrectResponseException("Incorrect response! Response must be from ItemOrderBookResponse type.");
        }

        ItemOrderBookResponse response;

        try {
            response = auroraResponse.getMessage().unpack(ItemOrderBookResponse.class);
        } catch (InvalidProtocolBufferException e) {
            LOGGER.error("Unable to parse Any to desired class: {}", e.getMessage());
            throw new IncorrectResponseException("Incorrect response! Response must be from ItemOrderBookResponse type.");
        }

        return response;
    }

    public AuroraServiceGrpc.AuroraServiceBlockingStub getBlockingStub() {

        return blockingStub;
    }
    public AvailabilityResponse checkAvailability(String itemName, double price, long quantity, Origin origin) {
        System.out.println("In Calculator-AuroraService buyProduct() method");
        ProductBuyRequest productBuyRequest = ProductBuyRequest.newBuilder()
                .setItemName(itemName)
                .setItemQuantity(quantity)
                .setItemPrice(price)
                .setOrigin(origin)
                .build();
        AvailabilityResponse availabilityResponse = getBlockingStub().checkAvailability(productBuyRequest);

        System.out.println("In Calculator-AuroraService buyProduct() method response -> " + availabilityResponse.toString());
        return availabilityResponse;
    }

    public void sellProduct(String itemName, double itemPrice, long itemQuantity, String itemOrigin, long itemTimestamp) {
        ProductSellRequest sellRequest = ProductSellRequest.newBuilder()
                .setItemName(itemName)
                .setItemPrice(itemPrice)
                .setItemQuantity(itemQuantity)
                .setMarketName(itemOrigin)
                .setTimestamp(itemTimestamp)
                .build();
        getBlockingStub().sellProduct(sellRequest);
    }

    public void buyProduct(String itemName, double itemPrice, long itemQuantity, String itemOrigin, long itemTimestamp) {
        // string itemName = 1;
        //  int64 itemQuantity = 2;
        //  double itemPrice = 3;
        //  Origin origin = 4;
        ProductBuyRequest buyRequest = ProductBuyRequest.newBuilder()
                .setItemName(itemName)
                .setItemQuantity(itemQuantity)
                .setItemPrice(itemPrice)
                .setOrigin(Origin.valueOf(itemOrigin))
                .setTimestamp(itemTimestamp)
                .build();
        getBlockingStub().buyProduct(buyRequest);
    }

    private Aurora.AuroraResponse getAuroraResponse(String message) {
        LOGGER.debug("In getAuroraResponse private method");

        Aurora.AuroraRequest request = buildAuroraRequest(message);

        return getBlockingStub().request(request);
    }

    private Aurora.AuroraRequest buildAuroraRequest(String message) {
        LOGGER.debug("In buildAuroraRequest private method");

        LOGGER.debug("Building request with parameter {}.", message);
        return Aurora.AuroraRequest
                .newBuilder()
                .setTopic(ORDERBOOK_TOPIC_PREFIX + message)
                .setClientId(CLIENT_ID)
                .build();
    }
}
