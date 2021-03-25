package com.market.banica.aurora.manager;

import com.aurora.Aurora;
import com.google.common.base.CharMatcher;
import com.market.banica.aurora.channel.OrderbookChannelManager;
import com.orderbook.InterestsRequest;
import com.orderbook.InterestsResponse;
import com.orderbook.ItemOrderBookRequest;
import com.orderbook.ItemOrderBookResponse;
import com.orderbook.OrderBookServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import jdk.internal.vm.compiler.collections.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Class handles blocking requests to aurora.
 */
@Component
public class RequestManager {

    @Autowired
    private OrderbookChannelManager orderbookChannel;

    private final String ORDERBOOK_PREFIX;

    @Autowired
    public RequestManager(@Value("${orderbook.prefix}") String orderbookPrefix) {
        ORDERBOOK_PREFIX = orderbookPrefix;

    }

    public void handleRequest(Aurora.AuroraRequest request, StreamObserver<Aurora.AuroraResponse> responseObserver) {
        if (request.getTopic().contains(ORDERBOOK_PREFIX)) {
            this.handleOrderbookRequest(request, responseObserver);
        } else {
            responseObserver.onError(new RuntimeException("request not supported"));
        }
    }

    private void handleOrderbookRequest(Aurora.AuroraRequest request, StreamObserver<Aurora.AuroraResponse> responseObserver) {
        OrderBookServiceGrpc.OrderBookServiceBlockingStub blockingStub = this.createOrderbookBlockingStub(orderbookChannel.getChannel());

        if (CharMatcher.is('/').countIn(request.getTopic()) > 1) {

            Pair<String, Long> pair = this.harvestItemRequestData(request);

            ItemOrderBookResponse orderBookResponse = sendItemOrderBookResponse(request, blockingStub, pair);

            Aurora.AuroraResponse auroraResponse = Aurora.AuroraResponse.newBuilder().
                    setItemOrderBookResponse(orderBookResponse).
                    build();

            sentAuroraResponse(auroraResponse, responseObserver);
        } else {

            final String[] interests = request.getTopic().split("/")[1].split(",");

            InterestsResponse interestsResponse = sendInterestsResponse(request, blockingStub, interests);

            Aurora.AuroraResponse auroraResponse = Aurora.AuroraResponse.newBuilder()
                    .setInterestsResponse(interestsResponse)
                    .build();

            sentAuroraResponse(auroraResponse, responseObserver);
        }
    }


    private void sentAuroraResponse(Aurora.AuroraResponse response, StreamObserver<Aurora.AuroraResponse> responseObserver) {
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private InterestsResponse sendInterestsResponse(Aurora.AuroraRequest request, OrderBookServiceGrpc.OrderBookServiceBlockingStub blockingStub, String[] interests) {
        return blockingStub.announceItemInterest(InterestsRequest.newBuilder()
                .addAllItemNames(Arrays.asList(interests))
                .setClientId(request.getClientId())
                .build());
    }

    private ItemOrderBookResponse sendItemOrderBookResponse(Aurora.AuroraRequest request, OrderBookServiceGrpc.OrderBookServiceBlockingStub blockingStub, Pair<String, Long> pair) {
        return blockingStub.getOrderBookItemLayers(ItemOrderBookRequest.newBuilder()
                .setClientId(request.getClientId())
                .setItemName(pair.getLeft())
                .setQuantity(pair.getRight())
                .build());
    }

    private OrderBookServiceGrpc.OrderBookServiceBlockingStub createOrderbookBlockingStub(ManagedChannel channel) {
        return OrderBookServiceGrpc.newBlockingStub(channel);
    }

    private Pair<String, Long> harvestItemRequestData(Aurora.AuroraRequest request) {
        String topic = request.getTopic();

        String[] split = topic.split("/");

        String productName = split[1];

        Long quantity = Long.parseLong(split[2]);

        return Pair.create(productName, quantity);
    }


}
