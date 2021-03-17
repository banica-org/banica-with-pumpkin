package com.market.banica.order.book;

import com.orderbook.ItemID;
import com.orderbook.ItemOrderBookRequest;
import com.orderbook.ItemOrderBookResponse;
import com.orderbook.OrderBookLayer;
import com.orderbook.OrderBookServiceGrpc;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class OrderBookService extends OrderBookServiceGrpc.OrderBookServiceImplBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderBookService.class);

    private final ItemMarket itemMarket;

    @Autowired
    private OrderBookService(final ItemMarket itemMarket) {
        this.itemMarket = itemMarket;
    }

    @Override
    public void getOrderBookItemLayers(ItemOrderBookRequest request, StreamObserver<ItemOrderBookResponse> responseObserver) {

        Set<Item> result = itemMarket.getAllItemsByName(request.getItemName());

        responseObserver.onNext(ItemOrderBookResponse.newBuilder()
                .setItemName(request.getItemName())
                .addAllOrderbookLayers(result.stream()
                        .map(item -> OrderBookLayer.newBuilder()
                                .setPrice(item.getPrice())
                                .setQuantity(item.getQuantity())
                                .addAllItemIds(item.getItemIDs().stream()
                                        .map(itemID -> ItemID.newBuilder()
                                                .setId(itemID.getId())
                                                .setLocation(itemID.getLocation())
                                                .build())
                                        .collect(Collectors.toList()))
                                .build()).collect(Collectors.toList()))
                .build());


        responseObserver.onCompleted();

        LOGGER.info("Get orderbook item layers by client id: {}", request.getClientId());

    }

}