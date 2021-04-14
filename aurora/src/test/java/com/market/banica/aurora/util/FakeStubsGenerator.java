package com.market.banica.aurora.util;

import com.aurora.Aurora;
import com.aurora.AuroraServiceGrpc;
import com.google.protobuf.Any;
import com.market.MarketDataRequest;
import com.market.MarketServiceGrpc;
import com.market.TickResponse;
import com.orderbook.CancelSubscriptionRequest;
import com.orderbook.CancelSubscriptionResponse;
import com.orderbook.InterestsRequest;
import com.orderbook.InterestsResponse;
import com.orderbook.ItemOrderBookRequest;
import com.orderbook.ItemOrderBookResponse;
import com.orderbook.OrderBookServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class FakeStubsGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(FakeStubsGenerator.class);
    private static final Map<String, ManagedChannel> channels = new ConcurrentHashMap<>();

    private static final String AURORA_SERVER_NAME = "auroraServer";
    private static final String ORDER_BOOK_SERVER_NAME = "orderBookServer";
    private static final String MARKET_SERVER_NAME = "marketServer";

    public static void createFakeServer(String serverName, GrpcCleanupRule grpcCleanup, ManagedChannel serverChannel) throws IOException {
        if (serverName.equalsIgnoreCase(AURORA_SERVER_NAME)) {
            grpcCleanup.register(InProcessServerBuilder
                    .forName(serverName)
                    .directExecutor()
                    .addService(fakeReceivingAuroraService())
                    .build()
                    .start());
        } else if (serverName.equalsIgnoreCase(ORDER_BOOK_SERVER_NAME)) {
            grpcCleanup.register(InProcessServerBuilder
                    .forName(serverName)
                    .directExecutor()
                    .addService(fakeReceivingOrderBookService())
                    .build()
                    .start());
        } else if (serverName.equalsIgnoreCase(MARKET_SERVER_NAME)) {
            grpcCleanup.register(InProcessServerBuilder
                    .forName(serverName)
                    .directExecutor()
                    .addService(fakeReceivingMarketService())
                    .build()
                    .start());
        } else {
            LOGGER.debug("Server with such name does not exist!");
        }

        grpcCleanup.register(serverChannel);
        serverChannel.getState(true);
    }

    public static void addChannel(String key, ManagedChannel value) {
        channels.put(key, value);
    }

    public static void shutDownAllChannels() {
        channels.values().forEach(FakeStubsGenerator::shutDownChannel);
    }

    private static void shutDownChannel(ManagedChannel channel) {
        try {
            channel.awaitTermination(100, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            LOGGER.error(e.getMessage());
        }
        channel.shutdownNow();
        LOGGER.debug("Channel was successfully stopped!");
    }

    private static AuroraServiceGrpc.AuroraServiceImplBase fakeReceivingAuroraService() {
        return new AuroraServiceGrpc.AuroraServiceImplBase() {
            @Override
            public void request(Aurora.AuroraRequest request, StreamObserver<Aurora.AuroraResponse> responseObserver) {
                Aurora.AuroraResponse response = Aurora.AuroraResponse
                        .newBuilder()
                        .setMessage(Any.pack(request))
                        .build();

                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }

            @Override
            public void subscribe(Aurora.AuroraRequest request, StreamObserver<Aurora.AuroraResponse> responseObserver) {
                for (int i = 0; i < 3; i++) {
                    Aurora.AuroraResponse response = Aurora.AuroraResponse
                            .newBuilder()
                            .setMessage(Any.pack(request))
                            .build();

                    responseObserver.onNext(response);
                }

                responseObserver.onCompleted();
            }
        };
    }

    private static MarketServiceGrpc.MarketServiceImplBase fakeReceivingMarketService() {
        return new MarketServiceGrpc.MarketServiceImplBase() {
            @Override
            public void subscribeForItem(MarketDataRequest request, StreamObserver<TickResponse> responseObserver) {
                responseObserver.onNext(TickResponse.newBuilder().build());
                responseObserver.onCompleted();
            }
        };
    }

    private static OrderBookServiceGrpc.OrderBookServiceImplBase fakeReceivingOrderBookService() {
        return new OrderBookServiceGrpc.OrderBookServiceImplBase() {
            @Override
            public void getOrderBookItemLayers(ItemOrderBookRequest request, StreamObserver<ItemOrderBookResponse> responseObserver) {
                responseObserver.onNext(ItemOrderBookResponse.newBuilder()
                        .setItemName(request.getItemName())
                        .build());

                responseObserver.onCompleted();
            }

            @Override
            public void announceItemInterest(InterestsRequest request, StreamObserver<InterestsResponse> responseObserver) {
                responseObserver.onNext(InterestsResponse.newBuilder()
                        .build());

                responseObserver.onCompleted();
            }

            @Override
            public void cancelItemSubscription(CancelSubscriptionRequest request, StreamObserver<CancelSubscriptionResponse> responseObserver) {
                responseObserver.onNext(CancelSubscriptionResponse.newBuilder()
                        .build());

                responseObserver.onCompleted();
            }
        };
    }
}


