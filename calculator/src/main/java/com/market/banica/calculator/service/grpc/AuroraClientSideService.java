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

        LOGGER.debug("Inside announceInterests method");
        LOGGER.debug("Building blocking stub");

        LOGGER.debug("Building request with parameters {}", productName);
        Aurora.AuroraRequest request = Aurora.AuroraRequest.newBuilder()
                .setTopic(ORDERBOOK_TOPIC_PREFIX + productName)
                .build();

        Aurora.AuroraResponse auroraResponse = blockingStub.request(request);

        if (!auroraResponse.hasInterestsResponse()) {
            throw new BadResponseException("Bad message from aurora service");
        }

    }

    public ItemOrderBookResponse getIngredient(String message, String clientId) {

        LOGGER.debug("Inside getIngredient method");
        LOGGER.debug("Building blocking stub");

        LOGGER.debug("Building request with parameters {}", message);
        Aurora.AuroraRequest request = Aurora.AuroraRequest.newBuilder()
                .setClientId(clientId)
                .setTopic(ORDERBOOK_TOPIC_PREFIX + message)
                .build();

        LOGGER.debug("Sending request to aurora.");

        Aurora.AuroraResponse auroraResponse = blockingStub.request(request);

        if (!auroraResponse.hasItemOrderBookResponse()) {
            throw new BadResponseException("Bad message from aurora service");
        }

        return auroraResponse.getItemOrderBookResponse();

    }
}
