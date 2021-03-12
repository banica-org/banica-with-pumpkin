package com.market.banica.calculator.service.grpcService;

import com.market.banica.aurora.AuroraServiceGrpc;
import com.market.banica.aurora.IngredientRequest;
import com.market.banica.aurora.IngredientResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;


/**
 * Date: 3/11/2021 Time: 12:35 PM
 * <p>
 *
 * @author Vladislav_Zlatanov
 */

@Service
public class AuroraClientSideService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuroraClientSideService.class);

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
                .build();
    }


    public IngredientResponse getIngredient(String itemName, int quantity) {

        LOGGER.debug("Inside getIngredient method");
        LOGGER.debug("Building blocking stub");
        AuroraServiceGrpc.AuroraServiceBlockingStub blockingStub = getBlockingStub();

        LOGGER.debug("Building request with parameters " + itemName + " " + quantity);
        IngredientRequest request = IngredientRequest.newBuilder()
                .setItemName(itemName)
                .setQuantity(quantity)
                .build();

        LOGGER.debug("Sending request to aurora.");

        return blockingStub.requestIngredient(request);
    }

    private AuroraServiceGrpc.AuroraServiceBlockingStub getBlockingStub() {
        return AuroraServiceGrpc.newBlockingStub(managedChannel);
    }

    /**
     * Shutting down channel before spring removes bean from application context
     */
    @PreDestroy
    private void shutdown() {
        LOGGER.info("Shutting down connection with Aurora.");
        managedChannel.shutdownNow();
    }

}
