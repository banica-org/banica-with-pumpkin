package com.market.banica.order.book.service.grpc.componentTests;

import com.aurora.Aurora;
import com.aurora.AuroraServiceGrpc;
import com.google.protobuf.Any;
import com.market.Origin;
import com.market.TickResponse;
import com.market.banica.order.book.OrderBookApplication;
import com.market.banica.order.book.exception.TrackingException;
import com.market.banica.order.book.service.grpc.AuroraClient;
import com.market.banica.order.book.service.grpc.OrderBookService;
import com.market.banica.order.book.service.grpc.componentTests.configuration.TestConfiguration;
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
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.io.IOException;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@SpringJUnitConfig
@SpringBootTest(classes = OrderBookApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("testOrderBookIT")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class OrderBookComponentIT {

    @SpyBean
    @Autowired
    private AuroraClient auroraClient;

    @Autowired
    private OrderBookService orderBookService;

    @Autowired
    private TestConfiguration testConfiguration;

    @Rule
    public GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    @LocalServerPort
    private int grpcMockPort;

    @Value(value = "${product.name}")
    private String productName;

    @Value(value = "${client.id}")
    private String clientId;

    @Value(value = "${product.price}")
    private double productPrice;

    @Value(value = "${market.topic.prefix}")
    private String orderBookTopicPrefix;

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
        ItemOrderBookResponse expected = generateItemOrderBookExpectedResponse();

        grpcCleanup.register(InProcessServerBuilder
                .forName(serverName).directExecutor().addService(testConfiguration.getGrpcOrderBookServiceItemLayers(expected))
                .build().start());

        // Act
        ItemOrderBookResponse reply = generateItemOrderBookResponseFromRequest();

        // Assert
        assertEquals(expected.getItemName(), reply.getItemName());
    }

    @Test
    void getOrderBookItemLayers_Should_ReturnError() throws IOException {
        // Arrange
        grpcCleanup.register(InProcessServerBuilder
                .forName(serverName).directExecutor().addService(testConfiguration.getEmptyOrderBookGrpcService())
                .build().start());

        // Act and Assert
        assertThrows(StatusRuntimeException.class, () -> blockingStub.getOrderBookItemLayers(ItemOrderBookRequest.newBuilder().build()));
    }

    @Test
    void announceItemInterest_Should_ReturnResponse() throws IOException {

        // Arrange
        grpcCleanup.register(channel);
        grpcCleanup.register(InProcessServerBuilder
                .forName(serverName).directExecutor()
                .addService(new OrderBookServiceGrpc.OrderBookServiceImplBase() {
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

        Aurora.AuroraResponse auroraResponse = generateAuroraResponse();
        AuroraServiceGrpc.AuroraServiceImplBase server = testConfiguration.getMockGrpcService(auroraResponse);

        grpcCleanup.register(InProcessServerBuilder.forName(serverNameTwo).directExecutor().addService(server).build().start());
        doReturn(asynchronousStub).when(auroraClient).getAsynchronousStub();

        // Act
        InterestsResponse response = blockingStub.announceItemInterest(InterestsRequest.newBuilder().setItemName(productName).build());

        // Assert
        verify(server).subscribe(requestCaptor.capture(), ArgumentMatchers.any());
        assertEquals(orderBookTopicPrefix + productName, requestCaptor.getValue().getTopic());
        assertTrue(response.isInitialized());
    }

    @Test
    void cancelItemSubscription_Should_ReturnResponse() throws IOException {

        // Arrange
        auroraClient.getCancellableStubs().put(productName, Context.current().withCancellation());
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
                .setItemName(productName)
                .setClientId(clientId)
                .build());

        // Assert
        assertNull(auroraClient.getCancellableStubs().get(productName));
    }

    private ItemOrderBookResponse generateItemOrderBookExpectedResponse() {
        ItemOrderBookResponse expected = ItemOrderBookResponse.newBuilder()
                .setItemName(productName)
                .addOrderbookLayers(0, OrderBookLayer.newBuilder()
                        .setPrice(productPrice)
                        .setOrigin(Origin.AMERICA)
                        .build())
                .build();
        return expected;
    }

    private ItemOrderBookResponse generateItemOrderBookResponseFromRequest() {
        ItemOrderBookResponse reply =
                blockingStub.getOrderBookItemLayers(ItemOrderBookRequest.newBuilder()
                        .setItemName(productName)
                        .setClientId(clientId)
                        .build());
        return reply;
    }

    private Aurora.AuroraResponse generateAuroraResponse() {
        Aurora.AuroraResponse auroraResponse = Aurora.AuroraResponse.newBuilder()
                .setMessage(Any.pack(TickResponse.newBuilder()
                        .setGoodName(productName)
                        .build()))
                .build();
        return auroraResponse;
    }
}



