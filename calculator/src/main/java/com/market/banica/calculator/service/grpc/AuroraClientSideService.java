package com.market.banica.calculator.service.grpc;


import com.aurora.Aurora;
import com.aurora.AuroraServiceGrpc;
import com.market.banica.calculator.exception.exceptions.BadResponseException;
import com.market.banica.common.ChannelRPCConfig;
import com.orderbook.ItemOrderBookResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.TimeUnit;


/**
 * Date: 3/11/2021 Time: 12:35 PM
 * <p>
 *
 * @author Vladislav_Zlatanov
 */

@Service
public class AuroraClientSideService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuroraClientSideService.class);

    private static final String ORDERBOOK_TOPIC_PREFIX = "orderbook/";

    /**
     * Channel for gRPC connection with aurora
     */
    private final ManagedChannel managedChannel;

    /**
     * @param host host where aurora is started
     * @param port port on the server.
     */
    @Autowired
    AuroraClientSideService(@Value("${aurora.server.host}") final String host,
                            @Value("${aurora.server.port}") final int port) {

        LOGGER.info("Setting up connection with Aurora.");

        managedChannel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .defaultServiceConfig(ChannelRPCConfig.getInstance().getServiceConfig())
                .enableRetry()
                .build();

    }


    public void announceInterests(List<String> products, String clientId) {

        LOGGER.debug("Inside announceInterests method");
        LOGGER.debug("Building blocking stub");
        AuroraServiceGrpc.AuroraServiceBlockingStub blockingStub = getBlockingStub();

        String message = String.join(",", products);

        LOGGER.debug("Building request with parameters {}", message);
        Aurora.AuroraRequest request = Aurora.AuroraRequest.newBuilder()
                .setClientId(clientId)
                .setTopic(ORDERBOOK_TOPIC_PREFIX + message)
                .build();

        Aurora.AuroraResponse auroraResponse = blockingStub.request(request);

        if (!auroraResponse.hasInterestsResponse()) {
            throw new BadResponseException("Bad message from aurora service");
        }

    }

    public ItemOrderBookResponse getIngredient(String message, String clientId) {

        LOGGER.debug("Inside getIngredient method");
        LOGGER.debug("Building blocking stub");
        AuroraServiceGrpc.AuroraServiceBlockingStub blockingStub = getBlockingStub();

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

    private AuroraServiceGrpc.AuroraServiceBlockingStub getBlockingStub() {
        return AuroraServiceGrpc.newBlockingStub(managedChannel);
    }

    /**
     * Shutting down channel before spring removes bean from application context
     */
    @PreDestroy
    private void shutdown() throws InterruptedException {
        LOGGER.info("Shutting down connection with Aurora.");
        managedChannel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

}
