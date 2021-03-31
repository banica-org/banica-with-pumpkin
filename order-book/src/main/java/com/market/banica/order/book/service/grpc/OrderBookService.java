package com.market.banica.order.book.service.grpc;

import com.aurora.Aurora;
import com.google.protobuf.Any;
import com.market.banica.order.book.exception.TrackingException;
import com.market.banica.order.book.model.Item;
import com.market.banica.order.book.model.ItemMarket;
import com.orderbook.CancelSubscriptionResponse;
import com.orderbook.InterestsResponse;
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
    private static final String REGEX = "/+";

    private final AuroraClient auroraClient;
    private final ItemMarket itemMarket;

    @Autowired
    private OrderBookService(final AuroraClient auroraClient, final ItemMarket itemMarket) {
        this.auroraClient = auroraClient;
        this.itemMarket = itemMarket;
    }

    @Override
    public void getOrderBookItemLayers(Aurora.AuroraRequest request, StreamObserver<Aurora.AuroraResponse> responseObserver) {
        String[] topicSplit = request.getTopic().split(REGEX);
        String itemName = topicSplit[1];
        long itemQuantity = Long.parseLong(topicSplit[2]);

        List<OrderBookLayer> requestedItem = itemMarket.getRequestedItem(itemName, itemQuantity);

        if (requestedItem.size()>0) {
            responseObserver.onNext(
                    Aurora.AuroraResponse.newBuilder().setMessage(Any.pack(
                            ItemOrderBookResponse.newBuilder()
                                    .setItemName(itemName)
                                    .addAllOrderbookLayers(requestedItem).build()))
                            .build()
            );
        } else {
            responseObserver.onNext(Aurora.AuroraResponse.newBuilder().setMessage(
                    Any.pack(
                            ItemOrderBookResponse.newBuilder()
                                    .setItemName(itemName)
                                    .build()
                    )).build());
        }
        responseObserver.onCompleted();

        LOGGER.info("Get orderbook item layers by client id: {}", request.getClientId());

    }

    @Override
    public void announceItemInterest(Aurora.AuroraRequest request, StreamObserver<Aurora.AuroraResponse> responseObserver) {
        String[] topicSplit = request.getTopic().split(REGEX);
        String itemName = topicSplit[1];
        String marketName = "";
        if (topicSplit.length == 3) {
            marketName = topicSplit[2];
        }
        try {

            auroraClient.startSubscription(itemName, request.getClientId(), marketName);
            responseObserver.onNext(Aurora.AuroraResponse.newBuilder().setMessage(
                    Any.pack(InterestsResponse.newBuilder().build())).build());
            responseObserver.onCompleted();
            LOGGER.info("Announce item interest by client id: {}", request.getClientId());

        } catch (TrackingException e) {

            LOGGER.warn("Announce item interest by client id: {} has failed with item: {}",
                    request.getClientId(), itemName);
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Invalid item name").asException());

        }
    }

    @Override
    public void cancelItemSubscription(Aurora.AuroraRequest request, StreamObserver<Aurora.AuroraResponse> responseObserver) {
        String[] topicSplit = request.getTopic().split(REGEX);
        String itemName = topicSplit[1];

        try {

            auroraClient.stopSubscription(itemName, request.getClientId());
            responseObserver.onNext(Aurora.AuroraResponse.newBuilder()
                    .setMessage(Any.pack(CancelSubscriptionResponse.newBuilder().build()))
                    .build());
            responseObserver.onCompleted();
            LOGGER.info("Cancel item subscription by client id: {}", request.getClientId());

        } catch (TrackingException e) {

            LOGGER.error("Cancel item subscription by client id: {} has failed with item: {}", request.getClientId(), itemName);
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Invalid item name").asException());
        }
    }
}