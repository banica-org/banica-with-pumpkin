package com.market.banica.calculator.service.grpc;


import com.aurora.Aurora;
import com.aurora.AuroraServiceGrpc;
import com.google.protobuf.InvalidProtocolBufferException;
import com.market.AvailabilityResponse;
import com.market.Origin;
import com.market.ProductBuyRequest;
import com.market.ProductSellRequest;
import com.market.TickResponse;
import com.market.banica.common.exception.IncorrectResponseException;
import com.orderbook.CancelSubscriptionResponse;
import com.orderbook.InterestsResponse;
import com.orderbook.ItemOrderBookResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Locale;


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
//        ProductBuyRequest productBuyRequest = ProductBuyRequest.newBuilder()
//                .setItemName(itemName)
//                .setItemPrice(price)
//                .setItemQuantity(quantity)
//                .setOrigin(origin)
//                .build();
// .                 setItemName(topicSplit[1])
//        //                .setItemPrice(Double.parseDouble(topicSplit[2]))
//        //                .setItemQuantity(Long.parseLong(topicSplit[3]))
//        //                .setMarketName(topicSplit[4]);
//        // available -> availability/itemName/price/quantity/origin
//        AvailabilityResponse availabilityResponse = getBlockingStub().checkAvailability(productBuyRequest);
        String originValue = origin.toString();

        Aurora.AuroraRequest availabilityRequest = Aurora.AuroraRequest.newBuilder()
                .setClientId(CLIENT_ID)
                .setTopic(String.format("market-%s/availability/%s/%f/%d/%s", originValue.toLowerCase(Locale.ROOT), itemName, price, quantity, originValue))
                .build();
        Aurora.AuroraResponse availabilityResponse = getBlockingStub().request(availabilityRequest);
        if (!availabilityResponse.getMessage().is(AvailabilityResponse.class)) {
            throw new IncorrectResponseException("Response is not correct!");
        }
        AvailabilityResponse response;
        try {
            response = availabilityResponse.getMessage().unpack(AvailabilityResponse.class);
        } catch (InvalidProtocolBufferException e) {
            throw new IncorrectResponseException("Response is not correct!");
        }
        System.out.println("In Calculator-AuroraService buyProduct() method response -> " + availabilityResponse.toString());
        return response;
    }

    public void sellProduct(String itemName, double itemPrice, long itemQuantity, String itemOrigin, long itemTimestamp) {
//        ProductSellRequest sellRequest = ProductSellRequest.newBuilder()
//                .setItemName(itemName)
//                .setItemPrice(itemPrice)
//                .setItemQuantity(itemQuantity)
//                .setMarketName(itemOrigin)
//                .setTimestamp(itemTimestamp)
//                .build();
//        getBlockingStub().sellProduct(sellRequest);

        //return -> market-europe/return/itemName/price/quantity/origin/timestamp
        String pattern = "market-%s/return/%s/%f/%d/%s/%d";
        Aurora.AuroraRequest availabilityRequest = Aurora.AuroraRequest.newBuilder()
                .setClientId(CLIENT_ID)
                .setTopic(String.format(pattern, itemOrigin.toLowerCase(Locale.ROOT),
                        itemName, itemPrice, itemQuantity, itemOrigin))
                .build();
        getBlockingStub().request(availabilityRequest);
    }

    public void buyProduct(String itemName, double itemPrice, long itemQuantity, String itemOrigin, long itemTimestamp) {
//        ProductBuyRequest buyRequest = ProductBuyRequest.newBuilder()
//                .setItemName(itemName)
//                .setItemPrice(itemPrice)
//                .setItemQuantity(itemQuantity)
//                .setOrigin(Origin.valueOf(itemOrigin))
//                .setTimestamp(itemTimestamp)
//                .build();
//        getBlockingStub().buyProduct(buyRequest);

        // buy -> market-europe/buy/itemName/price/quantity/origin/timestamp

        String pattern = "market-%s/buy/%s/%f/%d/%s/%d";
        Aurora.AuroraRequest availabilityRequest = Aurora.AuroraRequest.newBuilder()
                .setClientId(CLIENT_ID)
                .setTopic(String.format(pattern, itemOrigin.toLowerCase(Locale.ROOT),
                        itemName, itemPrice, itemQuantity, itemOrigin))
                .build();
        getBlockingStub().request(availabilityRequest);
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
