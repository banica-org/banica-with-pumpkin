package com.market.banica.calculator.service.grpc;


import com.aurora.Aurora;
import com.aurora.AuroraServiceGrpc;
import com.market.banica.calculator.exception.exceptions.BadResponseException;
import com.orderbook.ItemOrderBookResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class AuroraClientSideService {

    private static final String ORDERBOOK_TOPIC_PREFIX = "orderbook/";

    private static final Logger LOGGER = LoggerFactory.getLogger(AuroraClientSideService.class);

    private final AuroraServiceGrpc.AuroraServiceBlockingStub blockingStub;

    @Autowired
    public AuroraClientSideService(AuroraServiceGrpc.AuroraServiceBlockingStub blockingStub) {
        this.blockingStub = blockingStub;
    }

    public void announceInterests(String productName) {
        LOGGER.debug("Inside announceInterests method with parameter product name: {}",productName);

        Aurora.AuroraResponse auroraResponse = getAuroraResponse(productName);

        if (!auroraResponse.hasInterestsResponse()) {
            throw new BadResponseException("Bad message from aurora service");
        }
    }

    public void cancelSubscription(String productName) {
        LOGGER.debug("Inside cancelSubscription method with parameter product name: {}",productName);

        Aurora.AuroraResponse auroraResponse = getAuroraResponse(productName);

        if (!auroraResponse.hasCancelSubscriptionResponse()) {
            throw new BadResponseException("Bad message from aurora service");
        }
    }

    public ItemOrderBookResponse getIngredient(String productName, String clientId) {
        LOGGER.debug("Inside getIngredient method with parameter product name - {} and client id - {}"
                ,productName,clientId);

        Aurora.AuroraResponse auroraResponse = getAuroraResponse(productName);

        if (!auroraResponse.hasItemOrderBookResponse()) {
            throw new BadResponseException("Bad message from aurora service");
        }
        return auroraResponse.getItemOrderBookResponse();
    }

    public AuroraServiceGrpc.AuroraServiceBlockingStub getBlockingStub() {

        return blockingStub;
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
                .build();
    }
}
