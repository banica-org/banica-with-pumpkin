package com.market.banica.calculator.service.grpc;


import com.aurora.Aurora;
import com.aurora.AuroraServiceGrpc;
import com.google.protobuf.InvalidProtocolBufferException;
import com.market.banica.calculator.exception.exceptions.BadResponseException;
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

    private static final String AURORA_BAD_RESPONSE_MESSAGE = "Bad message from aurora service";

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
        Aurora.AuroraResponse auroraResponse = getAuroraResponse(CLIENT_ID, message);
        if (!auroraResponse.getMessage().is(InterestsResponse.class)) {
            throw new BadResponseException(AURORA_BAD_RESPONSE_MESSAGE);
        }
    }

    public void cancelSubscription(String productName) {
        LOGGER.debug("Inside cancelSubscription method with parameter product name: {}", productName);
        String message = String.format("%s=unsubscribe", productName);
        Aurora.AuroraResponse auroraResponse = getAuroraResponse(CLIENT_ID, message);

        if (!auroraResponse.getMessage().is(CancelSubscriptionResponse.class)) {
            throw new BadResponseException(AURORA_BAD_RESPONSE_MESSAGE);
        }
    }

    public ItemOrderBookResponse getIngredient(String productName, String clientId, Long quantity) {
        LOGGER.debug("Inside getIngredient method with parameter product name - {} and client id - {}"
                , productName, clientId);

        String message = String.format("%s/%d", productName, quantity);
        Aurora.AuroraResponse auroraResponse = getAuroraResponse(clientId, message);

        if (!auroraResponse.getMessage().is(ItemOrderBookResponse.class)) {
            throw new BadResponseException(AURORA_BAD_RESPONSE_MESSAGE);
        }

        ItemOrderBookResponse response;

        try {
            response = auroraResponse.getMessage().unpack(ItemOrderBookResponse.class);
        } catch (InvalidProtocolBufferException e) {
            LOGGER.error("Unable to parse Any to desired class: {}", e.getMessage());
            throw new BadResponseException(AURORA_BAD_RESPONSE_MESSAGE);
        }

        return response;
    }

    public AuroraServiceGrpc.AuroraServiceBlockingStub getBlockingStub() {

        return blockingStub;
    }

    private Aurora.AuroraResponse getAuroraResponse(String clientId, String message) {
        LOGGER.debug("In getAuroraResponse private method");

        Aurora.AuroraRequest request = buildAuroraRequest(clientId, message);

        return getBlockingStub().request(request);
    }

    private Aurora.AuroraRequest buildAuroraRequest(String clientId, String message) {
        LOGGER.debug("In buildAuroraRequest private method");

        LOGGER.debug("Building request with parameter {}.", message);
        return Aurora.AuroraRequest
                .newBuilder()
                .setClientId(clientId)
                .setTopic(ORDERBOOK_TOPIC_PREFIX + message)
                .build();
    }
}
