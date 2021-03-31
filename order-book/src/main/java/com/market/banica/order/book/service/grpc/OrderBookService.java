package com.market.banica.order.book.service.grpc;

import com.market.banica.order.book.exception.TrackingException;
import com.market.banica.order.book.model.Item;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class OrderBookService extends OrderBookServiceGrpc.OrderBookServiceImplBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderBookService.class);

    private final AuroraClient auroraClient;
    private final ItemMarket itemMarket;

    @Autowired
    private OrderBookService(final AuroraClient auroraClient, final ItemMarket itemMarket) {
        this.auroraClient = auroraClient;
        this.itemMarket = itemMarket;
    }

    @Override
    public void getOrderBookItemLayers(ItemOrderBookRequest request, StreamObserver<ItemOrderBookResponse> responseObserver) {

        Optional<Set<Item>> result = itemMarket.getItemSetByName(request.getItemName());

        List<OrderBookLayer> requestedItem = itemMarket.getRequestedItem(request);

        if (result.isPresent()) {
            responseObserver.onNext(ItemOrderBookResponse.newBuilder()
                    .setItemName(request.getItemName())
                    .addAllOrderbookLayers(requestedItem)
                    .build());
        } else {
            responseObserver.onNext(ItemOrderBookResponse.newBuilder()
                    .setItemName(request.getItemName())
                    .build());
        }
        responseObserver.onCompleted();

        LOGGER.info("Get orderbook item layers by client id: {}", request.getClientId());

    }

    @Override
    public void announceItemInterest(InterestsRequest request, StreamObserver<InterestsResponse> responseObserver) {

        try {

            auroraClient.startSubscription(request.getItemName(), request.getClientId());
            responseObserver.onNext(InterestsResponse.newBuilder().build());
            responseObserver.onCompleted();
            LOGGER.info("Announce item interest by client id: {}", request.getClientId());

        } catch (TrackingException e) {

            LOGGER.warn("Announce item interest by client id: {} has failed with item: {}", request.getClientId(), request.getItemName());
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Invalid item name").asException());

        }
    }

    @Override
    public void cancelItemSubscription(CancelSubscriptionRequest request, StreamObserver<CancelSubscriptionResponse> responseObserver) {

        try {

            auroraClient.stopSubscription(request.getItemName(), request.getClientId());
            responseObserver.onNext(CancelSubscriptionResponse.newBuilder().build());
            responseObserver.onCompleted();
            LOGGER.info("Cancel item subscription by client id: {}", request.getClientId());

        } catch (TrackingException e) {

            LOGGER.error("Cancel item subscription by client id: {} has failed with item: {}", request.getClientId(), request.getItemName());
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Invalid item name").asException());
        }
    }
}