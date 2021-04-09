package com.market.banica.order.book.service.grpc;

import com.market.banica.order.book.exception.TrackingException;
import com.market.banica.order.book.model.ItemMarket;
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
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@AllArgsConstructor
public class OrderBookService extends OrderBookServiceGrpc.OrderBookServiceImplBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderBookService.class);

    private final AuroraClient auroraClient;
    private final ItemMarket itemMarket;

    @Override
    public void getOrderBookItemLayers(ItemOrderBookRequest request, StreamObserver<ItemOrderBookResponse> responseObserver) {
        final String itemName = request.getItemName();
        final long itemQuantity = request.getQuantity();

        List<OrderBookLayer> requestedItem = itemMarket.getRequestedItem(itemName, itemQuantity);

        responseObserver.onNext(
                ItemOrderBookResponse.newBuilder()
                        .setItemName(itemName)
                        .addAllOrderbookLayers(requestedItem).build());
        responseObserver.onCompleted();

        LOGGER.info("Get orderbook item layers by client id: {}", request.getClientId());

    }

    @Override
    public void announceItemInterest(InterestsRequest request, StreamObserver<InterestsResponse> responseObserver) {
        final String itemName = request.getItemName();
        final String clientId = request.getClientId();

        try {

            auroraClient.startSubscription(itemName, clientId);
            responseObserver.onNext(InterestsResponse.newBuilder().build());
            responseObserver.onCompleted();
            LOGGER.info("Announce item interest by client id: {}", request.getClientId());

        } catch (TrackingException e) {

            LOGGER.warn("Announce item interest by client id: {} has failed with item: {}",
                    request.getClientId(), itemName);
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Invalid item name").asException());

        }
    }

    @Override
    public void cancelItemSubscription(CancelSubscriptionRequest request, StreamObserver<CancelSubscriptionResponse> responseObserver) {
        final String itemName = request.getItemName();
        final String clientId = request.getClientId();

        try {

            auroraClient.stopSubscription(itemName, clientId);
            responseObserver.onNext(CancelSubscriptionResponse.newBuilder().build());
            responseObserver.onCompleted();
            LOGGER.info("Cancel item subscription by client id: {}", clientId);

        } catch (TrackingException e) {

            LOGGER.error("Cancel item subscription by client id: {} has failed with item: {}", clientId, itemName);
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Invalid item name").asException());
        }
    }
}