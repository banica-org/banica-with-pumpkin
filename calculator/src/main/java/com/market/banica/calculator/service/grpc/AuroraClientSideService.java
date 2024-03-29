package com.market.banica.calculator.service.grpc;


import com.aurora.Aurora;
import com.aurora.AuroraServiceGrpc;
import com.google.protobuf.AbstractMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.market.AvailabilityResponse;
import com.market.BuySellProductResponse;
import com.market.Origin;
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

    public static final String SUBSCRIBE_FOR_PRODUCT_PATTERN = "%s/%s=subscribe";
    public static final String UNSUBSCRIBE_FOR_PRODUCT_PATTERN = "%s/%s=unsubscribe";
    public static final String GET_INGREDIENT_PATTERN = "%s/%s/%d";
    public static final String AVAILABILITY_REQUEST_PATTERN = "market-%s/availability/%s/%f/%d";
    public static final String RETURN_PENDING_PRODUCT_PATTERN = "market-%s/return/%s/%f/%d";
    public static final String SELL_PRODUCT_PATTERN = "market-%s/sell/%s/%f/%d/%s";
    public static final String BUY_PRODUCT_PATTERN = "market-%s/buy/%s/%f/%d";

    public static final String INCORRECT_RESPONSE_MESSAGE = "Incorrect response! Response must be from %s type.";

    private static final String ORDERBOOK_TOPIC_PREFIX = "orderbook";
    private static final String CLIENT_ID = "calculator";
    private static final Logger LOGGER = LoggerFactory.getLogger(AuroraClientSideService.class);

    private final AuroraServiceGrpc.AuroraServiceBlockingStub blockingStub;

    @Autowired
    public AuroraClientSideService(AuroraServiceGrpc.AuroraServiceBlockingStub blockingStub) {
        this.blockingStub = blockingStub;
    }

    public void announceInterests(String productName) {
        LOGGER.debug("Inside announceInterests method with parameter product name: {}", productName);

        String message = String.format(SUBSCRIBE_FOR_PRODUCT_PATTERN, ORDERBOOK_TOPIC_PREFIX, productName);
        Aurora.AuroraResponse auroraResponse = getAuroraResponse(message);

        if (!auroraResponse.getMessage().is(InterestsResponse.class)) {
            throw new IncorrectResponseException("Incorrect response! Response must be from InterestsResponse type.");
        }
    }

    public void cancelSubscription(String productName) {
        LOGGER.debug("Inside cancelSubscription method with parameter product name: {}", productName);

        String message = String.format(UNSUBSCRIBE_FOR_PRODUCT_PATTERN, ORDERBOOK_TOPIC_PREFIX, productName);
        Aurora.AuroraResponse auroraResponse = getAuroraResponse(message);

        if (!auroraResponse.getMessage().is(CancelSubscriptionResponse.class)) {
            throw new IncorrectResponseException("Incorrect response! Response must be from CancelSubscriptionResponse type.");
        }
    }

    public ItemOrderBookResponse getIngredient(String productName, String clientId, long quantity) {
        LOGGER.debug("Inside getIngredient method with parameter product name - {} and client id - {}", productName, clientId);

        String message = String.format(GET_INGREDIENT_PATTERN, ORDERBOOK_TOPIC_PREFIX, productName, quantity);
        Aurora.AuroraResponse auroraResponse = getAuroraResponse(message);
        ItemOrderBookResponse response = unpackAndValidateResponse(auroraResponse, ItemOrderBookResponse.class);

        return response;
    }

    public AuroraServiceGrpc.AuroraServiceBlockingStub getBlockingStub() {
        return blockingStub;
    }

    public AvailabilityResponse checkAvailability(String itemName, double price, long quantity, Origin origin) {
        String originValue = origin.toString();
        String message = String.format(AVAILABILITY_REQUEST_PATTERN, originValue.toLowerCase(Locale.ROOT), itemName, price, quantity);
        Aurora.AuroraResponse auroraResponse = getAuroraResponse(message);
        AvailabilityResponse availabilityResponse = unpackAndValidateResponse(auroraResponse, AvailabilityResponse.class);
        LOGGER.debug("Item with name {}, quantity={} and market name {} is available.",
                availabilityResponse.getItemName(),
                availabilityResponse.getItemQuantity(),
                availabilityResponse.getMarketName());

        return availabilityResponse;
    }

    public void returnPendingProductInMarket(String itemName, double itemPrice, long itemQuantity, String itemOrigin) {
        String message = (String.format(RETURN_PENDING_PRODUCT_PATTERN, itemOrigin.toLowerCase(Locale.ROOT), itemName, itemPrice, itemQuantity));
        Aurora.AuroraResponse auroraResponse = getAuroraResponse(message);
        BuySellProductResponse buySellProductResponse = unpackAndValidateResponse(auroraResponse, BuySellProductResponse.class);

        LOGGER.debug(buySellProductResponse.getMessage());
    }

    public void buyProductFromMarket(String itemName, double itemPrice, long itemQuantity, String itemOrigin) {
        String message = String.format(BUY_PRODUCT_PATTERN, itemOrigin.toLowerCase(Locale.ROOT), itemName, itemPrice, itemQuantity);
        Aurora.AuroraResponse auroraResponse = getAuroraResponse(message);
        BuySellProductResponse buySellProductResponse = unpackAndValidateResponse(auroraResponse, BuySellProductResponse.class);

        LOGGER.debug(buySellProductResponse.getMessage());
    }

    public String sellProductToMarket(String itemName, double itemPrice, long itemQuantity, String itemOrigin) {
        String message = String.format(SELL_PRODUCT_PATTERN, itemOrigin.toLowerCase(Locale.ROOT), itemName, itemPrice, itemQuantity, itemOrigin);
        Aurora.AuroraResponse auroraResponse = getAuroraResponse(message);
        BuySellProductResponse buySellProductResponse = unpackAndValidateResponse(auroraResponse, BuySellProductResponse.class);

        LOGGER.debug(buySellProductResponse.getMessage());

        return buySellProductResponse.getMessage();
    }

    @SuppressWarnings("unchecked")
    private <T> T unpackAndValidateResponse(Aurora.AuroraResponse auroraResponse, Class<T> type) {
        String exceptionMessage = String.format(INCORRECT_RESPONSE_MESSAGE, type.getSimpleName());

        if (!auroraResponse.getMessage().is((Class<? extends AbstractMessage>) type)) {
            throw new IncorrectResponseException(exceptionMessage);
        }

        Message unpack;
        try {
            unpack = auroraResponse.getMessage().unpack((Class<? extends AbstractMessage>) type);
        } catch (InvalidProtocolBufferException e) {
            LOGGER.error("Unable to parse Any to desired class: {}", e.getMessage());
            throw new IncorrectResponseException(exceptionMessage);
        }
        return type.cast(unpack);
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
                .setTopic(message)
                .setClientId(CLIENT_ID)
                .build();
    }
}
