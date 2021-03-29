package com.market.banica.order.book.service.grpc.componentTests;


import com.aurora.Aurora;
import com.aurora.AuroraServiceGrpc;
import com.market.Origin;
import com.market.TickResponse;
import com.market.banica.common.channel.GrpcChannel;
import com.market.banica.order.book.OrderBookApplication;
import com.market.banica.order.book.model.Item;
import com.market.banica.order.book.model.ItemMarket;
import com.market.banica.order.book.service.grpc.AuroraClient;
import com.market.banica.order.book.service.grpc.OrderBookService;
import com.orderbook.InterestsRequest;
import com.orderbook.ItemOrderBookRequest;
import com.orderbook.ItemOrderBookResponse;
import com.orderbook.OrderBookLayer;
import com.orderbook.OrderBookServiceGrpc;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.io.IOException;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;


@SpringJUnitConfig
@SpringBootTest(classes = OrderBookApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(JUnit4.class)
class OrderBookComponentIT {

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
    private ItemMarket itemMarket;

    private static ManagedChannel channel;
    private static ManagedChannel channelTwo;

    private String itemName;
    private String serverName;
    private String serverNameTwo;
    private OrderBookServiceGrpc.OrderBookServiceBlockingStub blockingStub;
    private AuroraServiceGrpc.AuroraServiceBlockingStub auroraBlockingStub;
    private OrderBookServiceGrpc.OrderBookServiceStub stub;

    @Value("${aurora.server.host}") String host;
    @Value("${aurora.server.port}") int port;

    @BeforeEach
    void setupChannel() {
        serverName = InProcessServerBuilder.generateName();
        serverNameTwo = InProcessServerBuilder.generateName();



        channel = new GrpcChannel(host,port).getManagedChannel();

        auroraBlockingStub = AuroraServiceGrpc.newBlockingStub(new GrpcChannel(host,port).getManagedChannel());

        blockingStub = OrderBookServiceGrpc.newBlockingStub(
                grpcCleanup.register(channel));

//                ManagedChannelBuilder.forAddress(serverName, grpcMockPort)
//                .usePlaintext()
//                .defaultServiceConfig(ChannelRPCConfig.getInstance().getServiceConfig())
//                .enableRetry()
//                .build();

        channelTwo = InProcessChannelBuilder.forName(serverNameTwo).executor(Executors.newSingleThreadExecutor()).build();
//                ManagedChannelBuilder.forAddress(serverNameTwo, 41114)
//                .usePlaintext()
//                .defaultServiceConfig(ChannelRPCConfig.getInstance().getServiceConfig())
//                .enableRetry()
//                .build();

    }

    @Test
    void getOrderBookItemLayers_Should_ReturnAUnaryResponse() throws IOException {

        // Arrange

        Map<String, TreeSet<Item>> allItems = new ConcurrentHashMap<>();
        TreeSet<Item> cheeseItems = new TreeSet<>();

        cheeseItems.add(new Item(2.6, 3, Origin.AMERICA));
        cheeseItems.add(new Item(2.5, 4, Origin.EUROPE));
        cheeseItems.add(new Item(2.7, 1, Origin.ASIA));
        allItems.put("cheese", cheeseItems);

        ItemOrderBookResponse expected = ItemOrderBookResponse.newBuilder()
                .setItemName("cheese")
                .addOrderbookLayers(0, OrderBookLayer.newBuilder()
                        .setPrice(2.6)
                        .setQuantity(3)
                        .setOrigin(Origin.AMERICA)
                        .build())
                .build();

        grpcCleanup.register(InProcessServerBuilder
                .forName(serverName).directExecutor().addService(new OrderBookServiceGrpc.OrderBookServiceImplBase() {
                    @Override
                    public void getOrderBookItemLayers(ItemOrderBookRequest request, StreamObserver<ItemOrderBookResponse> responseObserver) {
                        responseObserver.onNext(ItemOrderBookResponse.newBuilder()
                                .setItemName("cheese")
                                .addOrderbookLayers(0, OrderBookLayer.newBuilder()
                                        .setPrice(2.6)
                                        .setQuantity(3)
                                        .setOrigin(Origin.AMERICA)
                                        .build())
                                .build());

                        responseObserver.onCompleted();
                    }
                }).build().start());

        ItemOrderBookResponse reply =
                blockingStub.getOrderBookItemLayers(ItemOrderBookRequest.newBuilder()
                        .setItemName("cheese")
                        .setClientId("1")
                        .setQuantity(1)
                        .build());

        assertEquals(expected.getItemName(), reply.getItemName());

    }

    @Test
    void getOrderBookItemLayers_Should_ReturnError() throws IOException {

        // Arrange

        grpcCleanup.register(InProcessServerBuilder
                .forName(serverName).directExecutor().addService(new OrderBookServiceGrpc.OrderBookServiceImplBase() {
                })
                .build().start());
        // Assert
        assertThrows(StatusRuntimeException.class, () -> blockingStub.getOrderBookItemLayers(ItemOrderBookRequest.newBuilder().build()));
    }

    @Test
    void announceItemInterest_Should_ReturnResponse_When_Success() throws IOException {

        ArgumentCaptor<Aurora.AuroraRequest> requestCaptor = ArgumentCaptor.forClass(Aurora.AuroraRequest.class);

//        stub = OrderBookServiceGrpc.newStub(
//                grpcCleanup.register(InProcessChannelBuilder
//                        .forName("name").directExecutor().build()));


//        grpcCleanup.register(InProcessServerBuilder
//                .forName(serverName).directExecutor().addService(new OrderBookServiceGrpc.OrderBookServiceImplBase() {
//                    @Override
//                    public void announceItemInterest(InterestsRequest request, StreamObserver<InterestsResponse> responseObserver) {
//                        responseObserver.onNext(InterestsResponse.newBuilder().build());
//                    }
//                }).build().start());

        AuroraServiceGrpc.AuroraServiceImplBase server = mock(AuroraServiceGrpc.AuroraServiceImplBase.class,
                delegatesTo(new AuroraServiceGrpc.AuroraServiceImplBase() {

                    @Override
                    public void request(Aurora.AuroraRequest request, StreamObserver<Aurora.AuroraResponse> responseObserver) {

                        responseObserver.onNext(Aurora.AuroraResponse.newBuilder()
                                .setTickResponse(TickResponse.newBuilder()
                                        .setGoodName("cheese")
                                        .build())
                                .build());

                        responseObserver.onCompleted();
                    }
                }));


        grpcCleanup.register(channelTwo);

        grpcCleanup.register(InProcessServerBuilder.forName(serverNameTwo).directExecutor().addService(server).build().start());

        doReturn(channelTwo).when(auroraClient).getManagedChannel();

        blockingStub.announceItemInterest(InterestsRequest.newBuilder().setItemName("cheese").build());

        verify(server).request(requestCaptor.capture(), ArgumentMatchers.any());

        assertEquals("market/cheese", requestCaptor.getValue().getTopic());


    }
//
//    @Test
//    void announceItemInterest_Should_Return_Error() throws IOException {
//
//    }
//
//    @Test
//    void cancelItemSubscription() throws IOException {
//        grpcCleanup.register(InProcessServerBuilder
//                .forName(serverName).directExecutor().addService(new OrderBookServiceGrpc.OrderBookServiceImplBase() {
//                    @Override
//                    public void cancelItemSubscription(CancelSubscriptionRequest request, StreamObserver<CancelSubscriptionResponse> responseObserver) {
//                        responseObserver.onNext(CancelSubscriptionResponse.newBuilder()
//                                .build());
//
//                        responseObserver.onCompleted();
//                    }
//                }).build().start());
//
//        CancelSubscriptionResponse reply =
//                blockingStub.cancelItemSubscription(CancelSubscriptionRequest.newBuilder()
//                        .setItemName("cheese")
//                        .setClientId("1")
//                        .build());
//
//
//    }
}



