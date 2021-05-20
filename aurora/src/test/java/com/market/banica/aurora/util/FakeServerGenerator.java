package com.market.banica.aurora.util;

import com.aurora.Aurora;
import com.aurora.AuroraServiceGrpc;
import com.google.protobuf.Any;
import com.market.AvailabilityResponse;
import com.market.BuySellProductResponse;
import com.market.MarketDataRequest;
import com.market.MarketServiceGrpc;
import com.market.ProductBuySellRequest;
import com.market.TickResponse;
import com.orderbook.CancelSubscriptionRequest;
import com.orderbook.CancelSubscriptionResponse;
import com.orderbook.InterestsRequest;
import com.orderbook.InterestsResponse;
import com.orderbook.ItemOrderBookRequest;
import com.orderbook.ItemOrderBookResponse;
import com.orderbook.OrderBookServiceGrpc;
import io.grpc.BindableService;
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

public final class FakeServerGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(FakeServerGenerator.class);
    private static final Map<String, ManagedChannel> channels = new ConcurrentHashMap<>();

    private static final String AURORA_SERVER_CHANNEL_NAME = "auroraServerChannel";
    private static final String ORDER_BOOK_SERVER_CHANNEL_NAME = "orderBookServerChannel";
    private static final String MARKET_SERVER_CHANNEL_NAME = "marketServerChannel";

    private static final String AURORA_SERVER_NAME = "auroraServer";
    private static final String ORDER_BOOK_SERVER_NAME = "orderBookServer";
    private static final String MARKET_SERVER_NAME = "marketServer";

    public static void createFakeServer(String serverName, GrpcCleanupRule grpcCleanup, ManagedChannel serverChannel) throws IOException {
        if (serverName.equalsIgnoreCase(AURORA_SERVER_NAME) && !channels.containsKey(AURORA_SERVER_CHANNEL_NAME)) {
            registerFakeServer(serverName, grpcCleanup, fakeReceivingAuroraService());
        } else if (serverName.equalsIgnoreCase(ORDER_BOOK_SERVER_NAME) && !channels.containsKey(ORDER_BOOK_SERVER_CHANNEL_NAME)) {
            registerFakeServer(serverName, grpcCleanup, fakeReceivingOrderBookService());
        } else if (serverName.equalsIgnoreCase(MARKET_SERVER_NAME) && !channels.containsKey(MARKET_SERVER_CHANNEL_NAME)) {
            registerFakeServer(serverName, grpcCleanup, fakeReceivingMarketService());
        } else {
            LOGGER.debug("Server with such name does not exist!");
            return;
        }

        grpcCleanup.register(serverChannel);
        serverChannel.getState(true);
    }

    private static void registerFakeServer(String serverName, GrpcCleanupRule grpcCleanup, BindableService bindableService) throws IOException {
        grpcCleanup.register(InProcessServerBuilder
                .forName(serverName)
                .directExecutor()
                .addService(bindableService)
                .build()
                .start());
    }

    public static void addChannel(String key, ManagedChannel value) {
        channels.put(key, value);
    }

    public static void shutDownAllChannels() {
        channels.values().forEach(FakeServerGenerator::shutDownChannel);
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
            public StreamObserver<MarketDataRequest> subscribeForItem(StreamObserver<TickResponse> responseObserver) {
                responseObserver.onNext(TickResponse.newBuilder().setGoodName("banica").build());
                responseObserver.onCompleted();
                return new StreamObserver<MarketDataRequest>() {
                    @Override
                    public void onNext(MarketDataRequest marketDataRequest) {

                    }

                    @Override
                    public void onError(Throwable throwable) {

                    }

                    @Override
                    public void onCompleted() {

                    }
                };
            }

            @Override
            public void buyProduct(ProductBuySellRequest request, StreamObserver<BuySellProductResponse> responseObserver) {
                responseObserver.onNext(BuySellProductResponse.newBuilder().build());
                responseObserver.onCompleted();
            }

            @Override
            public void returnPendingProduct(ProductBuySellRequest request, StreamObserver<BuySellProductResponse> responseObserver) {
                responseObserver.onNext(BuySellProductResponse.newBuilder().build());
                responseObserver.onCompleted();
            }

            @Override
            public void checkAvailability(ProductBuySellRequest request, StreamObserver<AvailabilityResponse> responseObserver) {
                responseObserver.onNext(AvailabilityResponse.newBuilder().build());
                responseObserver.onCompleted();
            }

            @Override
            public void sellProduct(ProductBuySellRequest request, StreamObserver<BuySellProductResponse> responseObserver) {
                responseObserver.onNext(BuySellProductResponse.newBuilder().build());
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


