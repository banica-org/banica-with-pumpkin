package com.market.banica.order.book.service.grpc;

import com.market.banica.common.exception.TrackingException;
import com.market.banica.common.validator.DataValidator;
import com.market.banica.order.book.model.ItemMarket;
import com.market.banica.order.book.util.InterestsPersistence;
import com.orderbook.CancelSubscriptionRequest;
import com.orderbook.CancelSubscriptionResponse;
import com.orderbook.InterestsRequest;
import com.orderbook.InterestsResponse;
import com.orderbook.ItemOrderBookRequest;
import com.orderbook.ItemOrderBookResponse;
import com.orderbook.OrderBookLayer;
import com.orderbook.OrderBookServiceGrpc;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class OrderBookService extends OrderBookServiceGrpc.OrderBookServiceImplBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderBookService.class);

    private final ExecutorService subscriptionExecutor = Executors.newSingleThreadExecutor();

    private final AuroraClient auroraClient;
    private final ItemMarket itemMarket;

    private final InterestsPersistence interestsPersistence;
    private final Map<String, Set<String>> interestsMap = new HashMap<>();

    @Autowired
    public OrderBookService(AuroraClient auroraClient, ItemMarket itemMarket,
                            @Value("${orderbook.interests.file.name}") final String interestsFileName)
            throws IOException, TrackingException {
        this.auroraClient = auroraClient;
        this.itemMarket = itemMarket;
        this.interestsPersistence = new InterestsPersistence(interestsFileName, interestsMap);
        startPersistedInterests();
    }

    @Override
    public void getOrderBookItemLayers(ItemOrderBookRequest request, StreamObserver<ItemOrderBookResponse> responseObserver) {
        final String itemName = request.getItemName();
        DataValidator.validateIncomingData(itemName);

        final long itemQuantity = request.getQuantity();

        List<OrderBookLayer> requestedItem = itemMarket.getRequestedItem(itemName, itemQuantity);

        responseObserver.onNext(
                ItemOrderBookResponse.newBuilder()
                        .setItemName(itemName)
                        .addAllOrderbookLayers(requestedItem).build());
        responseObserver.onCompleted();

        LOGGER.debug("Get orderbook item layers by client id: {}", request.getClientId());

    }

    @Override
    public void announceItemInterest(InterestsRequest request, StreamObserver<InterestsResponse> responseObserver) {
        final String itemName = request.getItemName();
        final String clientId = request.getClientId();

        DataValidator.validateIncomingData(itemName);
        DataValidator.validateIncomingData(clientId);

        subscriptionExecutor.execute(() -> {
            try {
                interestsMap.putIfAbsent(clientId, new HashSet<>());
                interestsMap.get(clientId).add(itemName);
                interestsPersistence.persistInterests();

                auroraClient.startSubscription(itemName, clientId);
            } catch (TrackingException e) {
                LOGGER.warn("Announce item interest by client id: {} has failed with item: {}", clientId, itemName);
                responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Invalid item name").asException());
            } catch (IOException e) {
                LOGGER.error("Could not persist to back up file: {}", e.getMessage());
                responseObserver.onError(Status.ABORTED.withDescription("Interests persistence failed").asException());
            }
        });

        responseObserver.onNext(InterestsResponse.newBuilder().build());
        responseObserver.onCompleted();
        LOGGER.info("Announce \"{}\" interest by client id: {}", itemName, clientId);

    }

    @Override
    public void cancelItemSubscription(CancelSubscriptionRequest request, StreamObserver<CancelSubscriptionResponse> responseObserver) {
        final String itemName = request.getItemName();
        final String clientId = request.getClientId();

        DataValidator.validateIncomingData(itemName);
        DataValidator.validateIncomingData(clientId);

        subscriptionExecutor.execute(() -> {
            try {
                if (interestsMap.get(clientId) != null && interestsMap.get(clientId).remove(itemName)) {
                    interestsPersistence.persistInterests();

                    auroraClient.stopSubscription(itemName, clientId);
                    LOGGER.info("Cancel item subscription by client id: {}", clientId);
                } else {
                    throw new TrackingException("Item is already being tracked!");
                }
            } catch (TrackingException e) {
                LOGGER.error("Cancel item subscription by client id: {} has failed with item: {}", clientId, itemName);
                responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Invalid item name").asException());
            } catch (IOException e) {
                LOGGER.error("Could not persist to back up file: {}", e.getMessage());
                responseObserver.onError(Status.ABORTED.withDescription("Interests persistence failed").asException());
            }
        });

        responseObserver.onNext(CancelSubscriptionResponse.newBuilder().build());
        responseObserver.onCompleted();
        LOGGER.info("Stopped \"{}\" interest by client id: {}", itemName, clientId);

    }

    private void startPersistedInterests() throws IOException, TrackingException {
        this.interestsPersistence.loadInterests();
        for (Map.Entry<String, Set<String>> clientEntry : interestsMap.entrySet()) {
            for (String interest : clientEntry.getValue()) {
                auroraClient.startSubscription(interest, clientEntry.getKey());
            }
        }
    }

}