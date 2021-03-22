package com.market.banica.order.book;

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

    private final ItemMarketService itemMarketService;

    @Autowired
    private OrderBookService(final ItemMarketService itemMarketService) {
        this.itemMarketService = itemMarketService;
    }

    @Override
    public void getOrderBookItemLayers(ItemOrderBookRequest request, StreamObserver<ItemOrderBookResponse> responseObserver) {
        Set<Item> result = itemMarketService.getItemSetByName(request.getItemName());
        responseObserver.onNext(ItemOrderBookResponse.newBuilder()
                .setItemName(request.getItemName())
                .addAllOrderbookLayers(result.stream()
                        .map(item -> OrderBookLayer.newBuilder()
                                .setPrice(item.getPrice())
                                .setQuantity(item.getQuantity())
                                .setOrigin(item.getOrigin())
                                .build()).collect(Collectors.toList()))
                .build());
        responseObserver.onCompleted();
        LOGGER.info("Get orderbook item layers by client id: {}", request.getClientId());
    }

}