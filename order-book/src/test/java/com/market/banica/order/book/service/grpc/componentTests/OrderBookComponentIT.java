package com.market.banica.order.book.service.grpc.componentTests;

import com.aurora.Aurora;
import com.aurora.AuroraServiceGrpc;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.market.Origin;
import com.market.TickResponse;
import com.market.banica.order.book.OrderBookApplication;
import com.market.banica.order.book.exception.TrackingException;
import com.market.banica.order.book.service.ChannelRPCConfig;
import com.market.banica.order.book.service.grpc.AuroraClient;
import com.market.banica.order.book.service.grpc.OrderBookService;
import com.market.banica.order.book.service.grpc.componentTests.configuration.TestConfiguration;
import com.orderbook.CancelSubscriptionResponse;
import com.orderbook.InterestsResponse;
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
import lombok.SneakyThrows;
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

    @Value(value = "${market.name}")
    private String marketName;

    @Value(value = "${product.name}")
    private String productName;

    @Value(value = "${client.id}")
    private String clientId;

    @Value(value = "${product.price}")
    private double productPrice;

    @Value(value = "${product.quantity}")
    private int productQuantity;

    @Value(value = "${market.topic.prefix}")
    private String marketTopicPrefix;

    @Value(value = "${orderbook.topic.prefix}")
    private String orderBookTopicPrefix;

    private ItemOrderBookResponse itemOrderBookResponse;

    private static ManagedChannel channel;
    private static ManagedChannel channelTwo;

    private String serverName;
    private String serverNameTwo;
    private OrderBookServiceGrpc.OrderBookServiceBlockingStub blockingStub;
    private AuroraServiceGrpc.AuroraServiceStub asynchronousStub;

    private final static String DELIMITER = "/";

    @BeforeEach
    void setupChannel() {
        serverName = InProcessServerBuilder.generateName();
        serverNameTwo = InProcessServerBuilder.generateName();

        channel = InProcessChannelBuilder.forName(serverName).defaultServiceConfig(ChannelRPCConfig.getInstance().getServiceConfig())
                .enableRetry().maxRetryAttempts(10).directExecutor().build();


        channelTwo = InProcessChannelBuilder.forName(serverNameTwo).executor(Executors.newSingleThreadExecutor()).build();

        asynchronousStub = AuroraServiceGrpc.newStub(channelTwo);
        blockingStub = OrderBookServiceGrpc.newBlockingStub(grpcCleanup.register(channel));
    }

    @Test
    public void auroraServiceToOrderBookRequestsRetryExecutesWithSuccess() throws InvalidProtocolBufferException {
        //Arrange
        Aurora.AuroraResponse response = generateItemOrderBookExpectedResponse();

        new Thread(() -> {
            try {
                Thread.sleep(1000);
                grpcCleanup.register(InProcessServerBuilder
                        .forName(serverName).directExecutor().addService(testConfiguration.getGrpcOrderBookServiceItemLayers(response))
                        .build().start());
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }).start();

        Aurora.AuroraRequest auroraRequest = Aurora.AuroraRequest.newBuilder()
                .setTopic(orderBookTopicPrefix + productName + DELIMITER + productQuantity).setClientId(clientId).build();

        //Act
        Aurora.AuroraResponse auroraResponse = blockingStub.getOrderBookItemLayers(auroraRequest);

        ItemOrderBookResponse orderBookItemLayersResponse = null;
        try {
            orderBookItemLayersResponse = auroraResponse.getMessage().unpack(ItemOrderBookResponse.class);
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }

        ItemOrderBookResponse expected = generateItemOrderBookExpectedResponse().getMessage().unpack(ItemOrderBookResponse.class);

        //Assert
        assertEquals(expected, orderBookItemLayersResponse);
    }

    @Test
    void getOrderBookItemLayers_Should_ReturnAUnaryResponse() throws IOException {
        // Arrange
        Aurora.AuroraResponse expected = generateItemOrderBookExpectedResponse();

        grpcCleanup.register(InProcessServerBuilder
                .forName(serverName).directExecutor().addService(testConfiguration.getGrpcOrderBookServiceItemLayers(expected))
                .build().start());

        ItemOrderBookResponse itemOrderBookResponse = null;
        if (expected.getMessage().is(ItemOrderBookResponse.class)) {
            itemOrderBookResponse = expected.getMessage().unpack(ItemOrderBookResponse.class);
        }
        // Act
        Aurora.AuroraResponse reply = generateItemOrderBookResponseFromRequest();
        ItemOrderBookResponse itemOrderBookResponseReply = null;
        if (reply.getMessage().is(ItemOrderBookResponse.class)) {
            itemOrderBookResponseReply = expected.getMessage().unpack(ItemOrderBookResponse.class);
        }
        // Assert
        assertEquals(itemOrderBookResponse.getItemName(), itemOrderBookResponseReply.getItemName());
    }

    @Test
    void getOrderBookItemLayers_Should_ReturnError() throws IOException {
        // Arrange
        grpcCleanup.register(InProcessServerBuilder
                .forName(serverName).directExecutor().addService(testConfiguration.getEmptyOrderBookGrpcService())
                .build().start());

        // Act and Assert
        assertThrows(StatusRuntimeException.class, () -> blockingStub.getOrderBookItemLayers(Aurora.AuroraRequest.newBuilder().build()));
    }

    @Test
    void announceItemInterest_Should_ReturnResponse() throws IOException {

        // Arrange
        grpcCleanup.register(channel);
        grpcCleanup.register(InProcessServerBuilder
                .forName(serverName).directExecutor()
                .addService(new OrderBookServiceGrpc.OrderBookServiceImplBase() {
                    @Override
                    public void announceItemInterest(Aurora.AuroraRequest request, StreamObserver<Aurora.AuroraResponse> responseObserver) {
                        try {
                            auroraClient.startSubscription(request.getTopic().split(DELIMITER)[1], request.getClientId());
                        } catch (TrackingException e) {
                            e.printStackTrace();
                        }
                        responseObserver.onNext(Aurora.AuroraResponse.newBuilder().setMessage(
                                Any.pack(InterestsResponse.newBuilder().build())).build());

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
        Aurora.AuroraResponse response = blockingStub
                .announceItemInterest(Aurora.AuroraRequest.newBuilder()
                        .setTopic(marketTopicPrefix + productName).build());

        // Assert
        verify(server).subscribe(requestCaptor.capture(), ArgumentMatchers.any());
        assertEquals(marketTopicPrefix + productName, requestCaptor.getValue().getTopic());
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
                    public void cancelItemSubscription(Aurora.AuroraRequest request, StreamObserver<Aurora.AuroraResponse> responseObserver) {
                        try {
                            auroraClient.stopSubscription(request.getTopic().split(DELIMITER)[1], request.getClientId());
                        } catch (TrackingException e) {
                            e.printStackTrace();
                        }
                        responseObserver.onNext(
                                Aurora.AuroraResponse.newBuilder().setMessage(
                                        Any.pack(CancelSubscriptionResponse.newBuilder().build())).build());
                        responseObserver.onCompleted();
                    }
                }).build().start());

        // Act
        blockingStub.cancelItemSubscription(Aurora.AuroraRequest.newBuilder()
                .setTopic(marketTopicPrefix + productName)
                .setClientId(clientId)
                .build());

        // Assert
        assertNull(auroraClient.getCancellableStubs().get(productName));
    }

    private Aurora.AuroraResponse generateItemOrderBookExpectedResponse() {
        ItemOrderBookResponse expected = ItemOrderBookResponse.newBuilder()
                .setItemName(productName)
                .addOrderbookLayers(0, OrderBookLayer.newBuilder()
                        .setPrice(productPrice)
                        .setOrigin(Origin.AMERICA)
                        .build())
                .build();
        return Aurora.AuroraResponse.newBuilder().setMessage(Any.pack(expected)).build();
    }

    private Aurora.AuroraResponse generateItemOrderBookResponseFromRequest() {
        Aurora.AuroraResponse reply =
                blockingStub.getOrderBookItemLayers(Aurora.AuroraRequest.newBuilder()
                        .setTopic(marketTopicPrefix + productName)
                        .setClientId(clientId)
                        .build());
        return reply;
    }

    private Aurora.AuroraResponse generateAuroraResponse() {
        TickResponse tick = TickResponse.newBuilder()
                .setGoodName(productName)
                .build();
        Aurora.AuroraResponse auroraResponse = Aurora.AuroraResponse.newBuilder()
                .setMessage(Any.pack(tick))
                .build();
        return auroraResponse;
    }
}



