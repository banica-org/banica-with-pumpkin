package com.market.banica.aurora.service;

import com.aurora.Aurora;
import com.aurora.AuroraServiceGrpc;
import com.google.common.base.CharMatcher;
import com.market.banica.common.ChannelRPCConfig;
import com.orderbook.InterestsRequest;
import com.orderbook.InterestsResponse;
import com.orderbook.ItemOrderBookRequest;
import com.orderbook.ItemOrderBookResponse;
import com.orderbook.OrderBookServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import javafx.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@Service
public class AuroraServiceImpl extends AuroraServiceGrpc.AuroraServiceImplBase {

    private static final Logger LOGGER = LogManager.getLogger(AuroraServiceImpl.class);
    private final ManagedChannel managedChannel;
    ManagedChannel orderBookChannel;

    @Autowired
    AuroraServiceImpl(@Value("${aurora.server.host}") final String host,
                      @Value("${orderbook.server.port}") final int port) {

        LOGGER.info("Setting up connection with Orderbook.");

        managedChannel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .defaultServiceConfig(ChannelRPCConfig.getInstance().getServiceConfig())
                .enableRetry()
                .build();

    }

    @Override
    public void request(Aurora.AuroraRequest request, StreamObserver<Aurora.AuroraResponse> responseObserver) {
        LOGGER.info("Received request from client {}.", request.getClientId());

        if (request.getTopic().contains("orderbook")) {
            if (CharMatcher.is('/').countIn(request.getTopic()) > 1){
                OrderBookServiceGrpc.OrderBookServiceBlockingStub stub = OrderBookServiceGrpc.newBlockingStub(this.managedChannel);

                Pair<String, Long> pair = this.harvestData(request);

                ItemOrderBookResponse orderBookResponse = stub.getOrderBookItemLayers(ItemOrderBookRequest.newBuilder()
                        .setClientId(request.getClientId())
                        .setItemName(pair.getKey())
                        .setQuantity(pair.getValue())
                        .build());

                responseObserver.onNext(Aurora.AuroraResponse.newBuilder().
                        setItemOrderBookResponse(orderBookResponse).
                        build());

                responseObserver.onCompleted();
            } else {
                OrderBookServiceGrpc.OrderBookServiceBlockingStub stub = OrderBookServiceGrpc.newBlockingStub(this.managedChannel);

                final String[] interests = request.getTopic().substring(request.getTopic().indexOf("/")).split(",");

                InterestsResponse interestsResponse = stub.announceItemInterest(InterestsRequest.newBuilder()
                        .addAllItemNames(Arrays.asList(interests))
                        .setClientId(request.getClientId())
                        .build());

                responseObserver.onNext(Aurora.AuroraResponse.newBuilder()
                        .setInterestsResponse(interestsResponse)
                        .build());

                responseObserver.onCompleted();
            }

        }

        super.request(request, responseObserver);
    }

    private Pair<String, Long> harvestData(Aurora.AuroraRequest request) {
        String topic = request.getTopic();

        String[] split = topic.split("/");

        String productName = split[1];

        Long quantity = Long.parseLong(split[2]);

        return new Pair<>(productName, quantity);
    }

    @Override
    public void subscribe(Aurora.AuroraRequest request, StreamObserver<Aurora.AuroraResponse> responseObserver) {
        super.subscribe(request, responseObserver);
    }
}
