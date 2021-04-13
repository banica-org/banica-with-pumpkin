package com.market.banica.order.book.service.grpc.componentTests;

import com.aurora.Aurora;
import com.aurora.AuroraServiceGrpc;
import com.google.protobuf.Any;
import com.market.MarketDataRequest;
import com.market.MarketServiceGrpc;
import com.market.Origin;
import com.market.TickResponse;
import com.market.banica.common.channel.ChannelRPCConfig;
import com.market.banica.order.book.OrderBookApplication;
import com.market.banica.order.book.exception.TrackingException;
import com.market.banica.order.book.model.Item;
import com.market.banica.order.book.model.ItemMarket;
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
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import org.checkerframework.checker.units.qual.A;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringJUnitConfig
@SpringBootTest(classes = OrderBookApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("testOrderBookIT")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class OrderBookComponentIT {

    @SpyBean
    @Autowired
    private AuroraClient auroraClient;

    @Mock
    private ItemMarket itemMarket;

    @InjectMocks
    private OrderBookService orderBookService;

    @Autowired
    private OrderBookService orderBookServiceReal;

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
    private String serverNameThree;
    private OrderBookServiceGrpc.OrderBookServiceBlockingStub blockingStub;
    private AuroraServiceGrpc.AuroraServiceStub asynchronousStub;
    private AuroraServiceGrpc.AuroraServiceBlockingStub auroraBlockingStub;

    private final static String DELIMITER = "/";
    private final static int MAX_RETRY_ATTEMPTS = 10;
    private final static int THREAD_SLEEP_TIME = 1000;


    @BeforeEach
    void setupChannel() {
        serverName = InProcessServerBuilder.generateName();
        serverNameTwo = InProcessServerBuilder.generateName();
        serverNameThree = InProcessServerBuilder.generateName();

        channel = InProcessChannelBuilder.forName(serverName).defaultServiceConfig(ChannelRPCConfig.getInstance().getServiceConfig())
                .enableRetry().maxRetryAttempts(MAX_RETRY_ATTEMPTS).directExecutor().build();

        ManagedChannel channelThree = InProcessChannelBuilder.forName(serverNameThree).defaultServiceConfig(ChannelRPCConfig.getInstance().getServiceConfig())
                .enableRetry().maxRetryAttempts(MAX_RETRY_ATTEMPTS).directExecutor().build();

        channelTwo = InProcessChannelBuilder.forName(serverNameTwo).executor(Executors.newSingleThreadExecutor()).build();

        asynchronousStub = AuroraServiceGrpc.newStub(channelTwo);
        auroraBlockingStub = AuroraServiceGrpc.newBlockingStub(channelThree);
        blockingStub = OrderBookServiceGrpc.newBlockingStub(grpcCleanup.register(channel));
//        normalStub = OrderBookServiceGrpc.newStub(grpcCleanup.register(channel));
    }


    @Test
    public void auroraServiceToOrderBookRequestsRetryExecutesWithSuccess() {
        //Arrange
        ItemOrderBookResponse response = generateItemOrderBookExpectedResponse();

        new Thread(() -> {
            try {
                Thread.sleep(THREAD_SLEEP_TIME);
                grpcCleanup.register(InProcessServerBuilder
                        .forName(serverName).directExecutor().addService(testConfiguration.getGrpcOrderBookServiceItemLayers(response))
                        .build().start());
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }).start();

        ItemOrderBookRequest auroraRequest = ItemOrderBookRequest.newBuilder()
                .setItemName(productName).setClientId(clientId).setQuantity(productQuantity).build();

        //Act
        ItemOrderBookResponse auroraResponse = blockingStub.getOrderBookItemLayers(auroraRequest);

        ItemOrderBookResponse expected = generateItemOrderBookExpectedResponse();

        //Assert
        assertEquals(expected, auroraResponse);
    }

    @Test
    public void calculatorToAuroraToOrderBookRequestsRetryExecutesWithSuccess() {
        //Arrange
        List<OrderBookLayer> layers = new ArrayList<>();
        layers.add(OrderBookLayer.newBuilder().setPrice(productPrice).setQuantity(productQuantity).setOrigin(Origin.AMERICA).build());

        when(itemMarket.getRequestedItem(productName, productQuantity)).thenReturn(layers);

        // Start OrderBook
        new Thread(() -> {
            try {
                Thread.sleep(THREAD_SLEEP_TIME);
                grpcCleanup.register(InProcessServerBuilder
                        .forName(serverName).directExecutor().addService(orderBookService)
                        .build().start());
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }).start();


        // Start Aurora
        new Thread(() -> {
            try {
                Thread.sleep(THREAD_SLEEP_TIME);
                grpcCleanup.register(InProcessServerBuilder
                        .forName(serverNameThree).directExecutor().addService(new AuroraServiceGrpc.AuroraServiceImplBase() {
                            @Override
                            public void request(Aurora.AuroraRequest request, StreamObserver<Aurora.AuroraResponse> responseObserver) {
                                String[] topicSplit = request.getTopic().split(DELIMITER);

                                ItemOrderBookRequest build = populateItemOrderBookRequest(request.getClientId(), topicSplit[1], Long.parseLong(topicSplit[2]));

                                ItemOrderBookResponse orderBookItemLayers = blockingStub.getOrderBookItemLayers(build);

                                responseObserver.onNext(Aurora.AuroraResponse
                                        .newBuilder()
                                        .setMessage(Any.pack(orderBookItemLayers))
                                        .build());
                                responseObserver.onCompleted();
                            }
                        })
                        .build().start());
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }).start();


        Aurora.AuroraRequest auroraRequest = Aurora.AuroraRequest.newBuilder().setTopic(orderBookTopicPrefix + productName +
                DELIMITER + productQuantity).build();

        //Act
        Aurora.AuroraResponse auroraResponse = auroraBlockingStub.request(auroraRequest);

        Aurora.AuroraResponse expected = Aurora.AuroraResponse.newBuilder().setMessage(Any.pack(generateItemOrderBookExpectedResponse())).build();

        //Assert
        assertEquals(expected, auroraResponse);
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
        assertEquals(marketTopicPrefix + productName, requestCaptor.getValue().getTopic());
        assertTrue(response.isInitialized());
    }

    @Test
    public void startMarket() throws IOException {
        //Arrange

        ReflectionTestUtils.setField(orderBookService, "auroraClient", auroraClient);

        String serverNameFour = InProcessServerBuilder.generateName();

        when(auroraClient.getAsynchronousStub()).thenReturn(asynchronousStub);

        ManagedChannel marketChannel = InProcessChannelBuilder.forName(serverNameFour)
                .defaultServiceConfig(ChannelRPCConfig.getInstance().getServiceConfig())
                .enableRetry().maxRetryAttempts(MAX_RETRY_ATTEMPTS).directExecutor().build();


        // Start OrderBook
        grpcCleanup.register(InProcessServerBuilder
                .forName(serverName).directExecutor().addService(orderBookService)
                .build().start());


        grpcCleanup.register(InProcessServerBuilder
                .forName(serverNameFour).directExecutor().addService(new MarketServiceGrpc.MarketServiceImplBase() {
                    @Override
                    public void subscribeForItem(MarketDataRequest request, StreamObserver<TickResponse> responseObserver) {

                        System.out.println("IN MARKET");
                        for (int i = 0; i < 10; i++) {
                            TickResponse tickResponse = TickResponse.newBuilder()
                                    .setGoodName(request.getGoodName())
                                    .setQuantity(i + 1)
                                    .setPrice(i + 1.5)
                                    .setTimestamp(System.currentTimeMillis())
                                    .setOrigin(Origin.forNumber((i % 4))).build();
                            responseObserver.onNext(tickResponse);
                        }
                    }

                }).build().start());

        // Start Aurora
        grpcCleanup.register(InProcessServerBuilder
                .forName(serverNameTwo).directExecutor().addService(new AuroraServiceGrpc.AuroraServiceImplBase() {
                    @Override
                    public void subscribe(Aurora.AuroraRequest request, StreamObserver<Aurora.AuroraResponse> responseObserver) {
                        String itemForSubscribing = request.getTopic().split("/")[1]; // market/eggs
                        System.out.println("IN Aurora");
                        MarketDataRequest marketDataRequest = MarketDataRequest.newBuilder()
                                .setClientId(request.getClientId())
                                .setGoodName(itemForSubscribing)
                                .build();

                        MarketServiceGrpc.newStub(marketChannel)
                                .subscribeForItem(marketDataRequest, new StreamObserver<TickResponse>() {
                                    @Override
                                    public void onNext(TickResponse tickResponse) {
                                        System.out.println(tickResponse.toString());
                                        Aurora.AuroraResponse wrap = Aurora.AuroraResponse.newBuilder()
                                                .setMessage(Any.pack(tickResponse))
                                                .build();
                                        responseObserver.onNext(wrap);
                                    }

                                    @Override
                                    public void onError(Throwable throwable) {

                                    }

                                    @Override
                                    public void onCompleted() {
                                        responseObserver.onCompleted();
                                    }
                                });
                    }
                })
                .build().start());


        InterestsRequest interestsRequest = InterestsRequest.newBuilder().setClientId("aurora").setItemName("eggs").build();

        // market/eggs
        InterestsResponse interestsResponse = blockingStub.announceItemInterest(interestsRequest);
//        ReflectionTestUtils.setField(orderBookService, "auroraClient", null);


        System.out.println(interestsResponse.toString());
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

    private ItemOrderBookRequest populateItemOrderBookRequest(String clientId, String itemName, long quantity) {
        return ItemOrderBookRequest.newBuilder()
                .setClientId(clientId)
                .setItemName(itemName)
                .setQuantity(quantity)
                .build();
    }

    private ItemOrderBookResponse generateItemOrderBookExpectedResponse() {
        ItemOrderBookResponse expected = ItemOrderBookResponse.newBuilder()
                .setItemName(productName)
                .addOrderbookLayers(0, OrderBookLayer.newBuilder()
                        .setPrice(productPrice)
                        .setOrigin(Origin.AMERICA)
                        .setQuantity(productQuantity)
                        .build())
                .build();
        return expected;
    }

    private ItemOrderBookResponse generateItemOrderBookResponseFromRequest() {
        ItemOrderBookResponse reply =
                blockingStub.getOrderBookItemLayers(ItemOrderBookRequest.newBuilder()
                        .setItemName(marketTopicPrefix + productName)
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



