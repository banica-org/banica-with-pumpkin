package com.market.banica.order.book.service.grpc.componentTests;


import com.aurora.Aurora;
import com.aurora.AuroraServiceGrpc;
import com.market.Origin;
import com.market.TickResponse;
import com.market.banica.order.book.OrderBookApplication;
import com.market.banica.order.book.exception.TrackingException;
import com.market.banica.order.book.service.grpc.AuroraClient;
import com.market.banica.order.book.service.grpc.OrderBookService;
import com.orderbook.CancelSubscriptionRequest;
import com.orderbook.CancelSubscriptionResponse;
import com.orderbook.InterestsRequest;
import com.orderbook.InterestsResponse;
import com.orderbook.ItemOrderBookRequest;
import com.orderbook.ItemOrderBookResponse;
import com.orderbook.OrderBookLayer;
import com.orderbook.OrderBookServiceGrpc;
import io.grpc.Context;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.io.IOException;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;


@SpringJUnitConfig
@SpringBootTest(classes = OrderBookApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(JUnit4.class)
class OrderBookComponentIT {
    public static final String ITEM_NAME = "cheese";
    public static final String CLIENT_ID = "1";
    public static final double PRICE = 2.6;

    @SpyBean
    @Autowired
    private AuroraClient auroraClient;

    @Autowired
    private OrderBookService orderBookService;

    @Rule
    public GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    @LocalServerPort
    private int grpcMockPort;

    private ItemOrderBookResponse itemOrderBookResponse;

    private static ManagedChannel channel;
    private static ManagedChannel channelTwo;

    private String serverName;
    private String serverNameTwo;
    private OrderBookServiceGrpc.OrderBookServiceBlockingStub blockingStub;
    private AuroraServiceGrpc.AuroraServiceStub asynchronousStub;

    @BeforeEach
    void setupChannel() {
        serverName = InProcessServerBuilder.generateName();
        serverNameTwo = InProcessServerBuilder.generateName();

        channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
        channelTwo = InProcessChannelBuilder.forName(serverNameTwo).executor(Executors.newSingleThreadExecutor()).build();

        asynchronousStub = AuroraServiceGrpc.newStub(channelTwo);
        blockingStub = OrderBookServiceGrpc.newBlockingStub(grpcCleanup.register(channel));
    }

    @Test
    void getOrderBookItemLayers_Should_ReturnAUnaryResponse() throws IOException {
        // Arrange
        ItemOrderBookResponse expected = generateResponse();

        grpcCleanup.register(InProcessServerBuilder
                .forName(serverName).directExecutor().addService(new OrderBookServiceGrpc.OrderBookServiceImplBase() {
                    @Override
                    public void getOrderBookItemLayers(ItemOrderBookRequest request, StreamObserver<ItemOrderBookResponse> responseObserver) {
                        responseObserver.onNext(ItemOrderBookResponse.newBuilder()
                                .setItemName(ITEM_NAME)
                                .addOrderbookLayers(0, OrderBookLayer.newBuilder()
                                        .setPrice(PRICE)
                                        .setOrigin(Origin.AMERICA)
                                        .build())
                                .build());

                        responseObserver.onCompleted();
                    }
                }).build().start());
        // Act
        ItemOrderBookResponse reply =
                blockingStub.getOrderBookItemLayers(ItemOrderBookRequest.newBuilder()
                        .setItemName(ITEM_NAME)
                        .setClientId(CLIENT_ID)
                        .build());
        // Assert
        assertEquals(expected.getItemName(), reply.getItemName());
    }

    @Test
    void getOrderBookItemLayers_Should_ReturnError() throws IOException {
        // Arrange
        grpcCleanup.register(InProcessServerBuilder
                .forName(serverName).directExecutor().addService(new OrderBookServiceGrpc.OrderBookServiceImplBase() {
                })
                .build().start());

        // Act and Assert
        assertThrows(StatusRuntimeException.class, () -> blockingStub.getOrderBookItemLayers(ItemOrderBookRequest.newBuilder().build()));
    }

    @Test
    void announceItemInterest_Should_ReturnResponse() throws IOException {
        // Arrange
        grpcCleanup.register(channel);
        grpcCleanup.register(InProcessServerBuilder
                .forName(serverName).directExecutor().addService(new OrderBookServiceGrpc.OrderBookServiceImplBase() {
                    @Override
                    public void announceItemInterest(InterestsRequest request, StreamObserver<InterestsResponse> responseObserver) {
                        try {
                            auroraClient.startSubscription(request.getItemName(), request.getClientId());
                        } catch (TrackingException e) {
                            e.printStackTrace();
                        }
                        responseObserver.onNext(InterestsResponse.newBuilder().build());
                        responseObserver.onCompleted();
                    }
                }).build().start());
        ArgumentCaptor<Aurora.AuroraRequest> requestCaptor = ArgumentCaptor.forClass(Aurora.AuroraRequest.class);
        grpcCleanup.register(channelTwo);
        AuroraServiceGrpc.AuroraServiceImplBase server = mock(AuroraServiceGrpc.AuroraServiceImplBase.class, delegatesTo(new AuroraServiceGrpc.AuroraServiceImplBase() {

            @Override
            public void subscribe(Aurora.AuroraRequest request, StreamObserver<Aurora.AuroraResponse> responseObserver) {
                responseObserver.onNext(Aurora.AuroraResponse.newBuilder()
                        .setTickResponse(TickResponse.newBuilder()
                                .setGoodName(ITEM_NAME)
                                .build())
                        .build());
                responseObserver.onCompleted();
            }
        }));
        grpcCleanup.register(InProcessServerBuilder.forName(serverNameTwo).directExecutor().addService(server).build().start());
        doReturn(asynchronousStub).when(auroraClient).getAsynchronousStub();
        // Act
        InterestsResponse response = blockingStub.announceItemInterest(InterestsRequest.newBuilder().setItemName(ITEM_NAME).build());
        // Assert
        verify(server).subscribe(requestCaptor.capture(), ArgumentMatchers.any());
        assertEquals("market/cheese", requestCaptor.getValue().getTopic());
        assertTrue(response.isInitialized());
    }

    @Test
    void cancelItemSubscription_Should_ReturnResponse() throws IOException {
        // Arrange
        auroraClient.getCancellableStubs().put(ITEM_NAME, Context.current().withCancellation());
        grpcCleanup.register(channel);
        grpcCleanup.register(InProcessServerBuilder
                .forName(serverName).directExecutor().addService(new OrderBookServiceGrpc.OrderBookServiceImplBase() {
                    @Override
                    public void cancelItemSubscription(CancelSubscriptionRequest request, StreamObserver<CancelSubscriptionResponse> responseObserver) {
                        try {
                            auroraClient.stopSubscription(request.getItemName(), request.getClientId());
                        } catch (TrackingException e) {
                            e.printStackTrace();
                        }
                        responseObserver.onNext(CancelSubscriptionResponse.newBuilder().build());
                        responseObserver.onCompleted();
                    }
                }).build().start());
        // Act
        blockingStub.cancelItemSubscription(CancelSubscriptionRequest.newBuilder()
                .setItemName(ITEM_NAME)
                .setClientId(CLIENT_ID)
                .build());
        // Assert
        assertNull(auroraClient.getCancellableStubs().get(ITEM_NAME));
    }

    private ItemOrderBookResponse generateResponse() {
        ItemOrderBookResponse expected = ItemOrderBookResponse.newBuilder()
                .setItemName(ITEM_NAME)
                .addOrderbookLayers(0, OrderBookLayer.newBuilder()
                        .setPrice(PRICE)
                        .setOrigin(Origin.AMERICA)
                        .build())
                .build();
        return expected;
    }
}



