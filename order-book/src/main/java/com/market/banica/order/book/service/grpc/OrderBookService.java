package com.market.banica.order.book.service.grpc;

import com.market.banica.order.book.exception.TrackingException;
import com.market.banica.order.book.model.Item;
import com.market.banica.order.book.model.ItemMarket;
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

import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

//TODO: IF CLASS DIFFRENT FROM MAIN GET FROM HERE
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

        if (result.isPresent()) {
            responseObserver.onNext(ItemOrderBookResponse.newBuilder()
                    .setItemName(request.getItemName())
                    .addAllOrderbookLayers(result.get().stream()
                            .map(item -> OrderBookLayer.newBuilder()
                                    .setPrice(item.getPrice())
                                    .setQuantity(item.getQuantity())
                                    .setOrigin(item.getOrigin())
                                    .build()).collect(Collectors.toList()))
                    .build());
        } else {
            responseObserver.onNext(ItemOrderBookResponse.newBuilder()
                    .setItemName(request.getItemName())
                    .addAllOrderbookLayers(new TreeSet<>())
                    .build());
        }


        responseObserver.onCompleted();

        LOGGER.info("Get orderbook item layers by client id: {}", request.getClientId());

    }

    @Override
    public void announceItemInterest(InterestsRequest request, StreamObserver<InterestsResponse> responseObserver) {

        try {
            LOGGER.info("request data : {}", request.getItemNamesList());
            auroraClient.updateItems(request.getItemNamesList(), request.getClientId());
            responseObserver.onNext(InterestsResponse.newBuilder().build());
            responseObserver.onCompleted();
            LOGGER.info("Announce item interests by client id: {}", request.getClientId());

        } catch (TrackingException e) {

            LOGGER.warn("Announce item interests by client id: {} has failed with items: {}", request.getClientId(), request.getItemNamesList());
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Invalid item list").asException());

        }

    }

}