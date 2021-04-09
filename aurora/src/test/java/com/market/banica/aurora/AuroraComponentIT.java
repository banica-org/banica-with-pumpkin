package com.market.banica.aurora;

import com.aurora.Aurora;
import com.aurora.AuroraServiceGrpc;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.market.MarketDataRequest;
import com.market.MarketServiceGrpc;
import com.market.TickResponse;
import com.market.banica.aurora.config.ChannelManager;
import com.market.banica.aurora.config.JMXConfig;
import com.market.banica.aurora.config.Publishers;
import com.market.banica.aurora.model.ChannelProperty;
import com.market.banica.aurora.service.AuroraServiceImpl;
import com.market.banica.common.util.ApplicationDirectoryUtil;
import com.orderbook.CancelSubscriptionRequest;
import com.orderbook.CancelSubscriptionResponse;
import com.orderbook.InterestsRequest;
import com.orderbook.InterestsResponse;
import com.orderbook.ItemOrderBookRequest;
import com.orderbook.ItemOrderBookResponse;
import com.orderbook.OrderBookServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.Rule;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.aurora.AuroraServiceGrpc.newStub;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


@SpringJUnitConfig
@SpringBootTest(classes = AuroraApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("testAuroraIT")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AuroraComponentIT {


    @Autowired
    private ChannelManager channelManager;

    @Autowired
    private Publishers publishers;

    @Autowired
    private JMXConfig jmxConfig;

    @Autowired
    private AuroraServiceImpl auroraService;

    @Rule
    public GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    @LocalServerPort
    private int grpcMockPort;

    private String receiverChannelName;
    private ManagedChannel receiverChannel;

    private String senderName;
    private ManagedChannel senderChannel;

    private AuroraServiceGrpc.AuroraServiceBlockingStub blockingStub;
    private AuroraServiceGrpc.AuroraServiceStub stub;

    private static String channelsBackupUrl;
    private static String publishersBackupUrl;

    private static Set<String> channelNames = new HashSet<>();


    @BeforeAll
    public static void getFilePath(@Value("${aurora.channels.file.name}") String channels,@Value("${aurora.channels.publishers}") String publishersFileName){
        channelsBackupUrl = channels;
        publishersBackupUrl = publishersFileName;

    }




    @AfterAll

    public static void cleanUp() throws IOException {
        ApplicationDirectoryUtil.getConfigFile(channelsBackupUrl).delete();
        ApplicationDirectoryUtil.getConfigFile(publishersBackupUrl).delete();
    }


    @BeforeEach
    public void setup() throws IOException, InterruptedException {
        senderName = InProcessServerBuilder.generateName();
        senderChannel = InProcessChannelBuilder
                .forName(senderName)
                .directExecutor()
                .build();

        receiverChannelName = InProcessServerBuilder.generateName();
        receiverChannel = InProcessChannelBuilder
                .forName(receiverChannelName)
                .executor(Executors.newSingleThreadExecutor()).build();

        channelNames.add(receiverChannelName);
        publishers.addPublisher("aurora");
    }

    @AfterEach
    public void tearDown(){
        publishers.deletePublisher("aurora");
    }


    @Test
    void request_Should_ForwardToReceiverResponse() throws IOException {
        //Arrange
        createFakeReplyingServer();
        createFakeSender();

        grpcCleanup.register(senderChannel);
        grpcCleanup.register(receiverChannel);

        receiverChannel.getState(true);
        channelManager.addChannel("aurora", receiverChannel);

        Aurora.AuroraRequest request = getAuroraRequest();

        //Act
        Aurora.AuroraResponse response = AuroraServiceGrpc
                .newBlockingStub(senderChannel)
                .request(request);

        //Assert
        assertEquals(request, response.getMessage().unpack(Aurora.AuroraRequest.class));
        channelManager.removeChannel("aurora");

    }

    @Test
    void request_Should_ForwardToReceiverError() throws IOException {
        //Arrange

        createFakeErrorServer();
        createFakeSender();

        grpcCleanup.register(senderChannel);
        grpcCleanup.register(receiverChannel);

        receiverChannel.getState(true);
        channelManager.addChannel("aurora", receiverChannel);

        Aurora.AuroraRequest request = getAuroraRequest();

        //Act & Assert
        assertThrows(StatusRuntimeException.class, () -> AuroraServiceGrpc
                .newBlockingStub(senderChannel)
                .request(request));

        channelManager.removeChannel("aurora");

    }

    @Test
    void request_Should_ForwardToReceiverError_NoExistingChannel() throws IOException {
        //Arrange
        createFakeErrorServer();
        createFakeSender();

        grpcCleanup.register(senderChannel);
        grpcCleanup.register(receiverChannel);

        receiverChannel.getState(true);

        Aurora.AuroraRequest request = getAuroraRequest();

        //Act & Assert
        assertThrows(StatusRuntimeException.class, () -> AuroraServiceGrpc
                .newBlockingStub(senderChannel)
                .request(request));
    }

    @Test
    void ItemOrderBookRequest_Should_ForwardToOrderbookResponse() throws IOException {
        //Arrange
        publishers.addPublisher("orderbook");
        createFakeOrderBookServer();
        createFakeSender();

        grpcCleanup.register(senderChannel);
        grpcCleanup.register(receiverChannel);

        receiverChannel.getState(true);
        channelManager.addChannel("orderbook", receiverChannel);

        Aurora.AuroraRequest itemOrderBookRequest = Aurora.AuroraRequest.newBuilder()
                .setClientId("fake calculator item orderbook request")
                .setTopic("orderbook/eggs/15").build();

        //Act
        Aurora.AuroraResponse response = AuroraServiceGrpc
                .newBlockingStub(senderChannel)
                .request(itemOrderBookRequest);

        //Assert
        assertTrue(response.getMessage().is(ItemOrderBookResponse.class));
        assertEquals("eggs",response.getMessage().unpack(ItemOrderBookResponse.class).getItemName());

        Aurora.AuroraRequest subscribeToItemRequest = Aurora.AuroraRequest.newBuilder()
                .setClientId("fake calculator subscribe request")
                .setTopic("orderbook/eggs=subscribe").build();

        Aurora.AuroraResponse subsrcibeResponse = AuroraServiceGrpc
                .newBlockingStub(senderChannel)
                .request(subscribeToItemRequest);

        assertTrue(subsrcibeResponse.getMessage().is(InterestsResponse.class));

        Aurora.AuroraRequest unsubscribeToItemRequest = Aurora.AuroraRequest.newBuilder()
                .setClientId("fake calculator subscribe request")
                .setTopic("orderbook/eggs=unsubscribe").build();

        Aurora.AuroraResponse unsubscribeResponse = AuroraServiceGrpc
                .newBlockingStub(senderChannel)
                .request(unsubscribeToItemRequest);

        assertTrue(unsubscribeResponse.getMessage().is(CancelSubscriptionResponse.class));

        channelManager.removeChannel("orderbook");
        publishers.deletePublisher("orderbook");

    }

    @Test
    void subscribe_Should_ForwardToReceiverResponse() throws IOException, InterruptedException {
        //Arrange

        createFakeReplyingServer();
        createFakeSender();



        grpcCleanup.register(senderChannel);
        grpcCleanup.register(receiverChannel);

        receiverChannel.getState(true);
        channelManager.addChannel("aurora", receiverChannel);

        Aurora.AuroraRequest request = getAuroraRequest();

        ArrayList<Aurora.AuroraResponse> expectedResponses = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Aurora.AuroraResponse response = Aurora.AuroraResponse
                    .newBuilder()
                    .setMessage(Any.pack(request))
                    .build();

            expectedResponses.add(response);
        }

        ArrayList<Aurora.AuroraResponse> receivedResponses = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        //Act
        AuroraServiceGrpc
                .newStub(senderChannel)
                .subscribe(request, new StreamObserver<Aurora.AuroraResponse>() {
                    @Override
                    public void onNext(Aurora.AuroraResponse response) {
                        receivedResponses.add(response);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        latch.countDown();
                    }

                    @Override
                    public void onCompleted() {
                        latch.countDown();
                    }
                });

        //Assert
        latch.await();
        assertEquals(expectedResponses, receivedResponses);
        channelManager.removeChannel("aurora");
    }

    @Test
    void subscribeToMarket_Should_ForwardToReceiverResponse() throws IOException, InterruptedException {
        //Arrange
        publishers.addPublisher("market");
        createFakeMarketServer();;
        createFakeSender();



        grpcCleanup.register(senderChannel);
        grpcCleanup.register(receiverChannel);

        receiverChannel.getState(true);
        channelManager.addChannel("market", receiverChannel);

        Aurora.AuroraRequest request = Aurora.AuroraRequest.newBuilder().setTopic("market/eggs").build();

        ArrayList<TickResponse> expectedResponses = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            TickResponse response = TickResponse.newBuilder().setGoodName("eggs").build();

            expectedResponses.add(response);
        }

        ArrayList<Aurora.AuroraResponse> receivedResponses = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        //Act
        AuroraServiceGrpc
                .newStub(senderChannel)
                .subscribe(request, new StreamObserver<Aurora.AuroraResponse>() {
                    @Override
                    public void onNext(Aurora.AuroraResponse response) {
                        receivedResponses.add(response);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        latch.countDown();
                    }

                    @Override
                    public void onCompleted() {
                        latch.countDown();
                    }
                });

        //Assert
        latch.await();
        List<TickResponse> unpackedResponses = receivedResponses.stream()
                .map(response -> {
                    try {
                        return response.getMessage().unpack(TickResponse.class);
                    } catch (InvalidProtocolBufferException e) {
                        e.printStackTrace();
                    }
                    return null;
                })
                .collect(Collectors.toList());
        assertEquals(expectedResponses, unpackedResponses);
        channelManager.removeChannel("market");
        publishers.deletePublisher("market");
    }

    @Test
    void subscribe_Should_ForwardToReceiverLessResponsesDueError() throws IOException, InterruptedException {
        //Arrange
        createFakeErrorServer();
        createFakeSender();

        System.out.println("CHANNEL NAME: " + senderName);

        grpcCleanup.register(senderChannel);
        grpcCleanup.register(receiverChannel);

        receiverChannel.getState(true);
        channelManager.addChannel("aurora", receiverChannel);

        Aurora.AuroraRequest request = getAuroraRequest();

        ArrayList<Aurora.AuroraResponse> expectedResponses = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Aurora.AuroraResponse response = Aurora.AuroraResponse
                    .newBuilder()
                    .setMessage(Any.pack(request))
                    .build();

            expectedResponses.add(response);
        }

        ArrayList<Aurora.AuroraResponse> receivedResponses = new ArrayList<>();

        CountDownLatch latch = new CountDownLatch(1);
        //Act
        newStub(senderChannel)
                .subscribe(request, new StreamObserver<Aurora.AuroraResponse>() {
                    @Override
                    public void onNext(Aurora.AuroraResponse response) {
                        receivedResponses.add(response);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        latch.countDown();
                    }

                    @Override
                    public void onCompleted() {
                        latch.countDown();
                    }
                });

        //Assert
        latch.await();
        assertNotEquals(expectedResponses, receivedResponses);
        assertEquals(3,receivedResponses.size());
        channelManager.removeChannel("aurora");
    }

    @Test
    void subscribe_Should_ForwardToReceiverNoResponses_NoExistingChannel() throws IOException, InterruptedException {
        //Arrange
        createFakeErrorServer();
        createFakeSender();

        grpcCleanup.register(senderChannel);
        grpcCleanup.register(receiverChannel);

        receiverChannel.getState(true);

        Aurora.AuroraRequest request = getAuroraRequest();

        ArrayList<Aurora.AuroraResponse> expectedResponses = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Aurora.AuroraResponse response = Aurora.AuroraResponse
                    .newBuilder()
                    .setMessage(Any.pack(request))
                    .build();

            expectedResponses.add(response);
        }

        ArrayList<Aurora.AuroraResponse> receivedResponses = new ArrayList<>();

        CountDownLatch latch = new CountDownLatch(1);
        //Act
        newStub(senderChannel)
                .subscribe(request, new StreamObserver<Aurora.AuroraResponse>() {
                    @Override
                    public void onNext(Aurora.AuroraResponse response) {
                        receivedResponses.add(response);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        latch.countDown();
                    }

                    @Override
                    public void onCompleted() {
                        latch.countDown();
                    }
                });

        //Assert
        latch.await();
        assertNotEquals(expectedResponses, receivedResponses);
        assertEquals(0,receivedResponses.size());
    }

    @Test
    void jmx_ChannelManager_FlowTest(){
        Map<String, ChannelProperty> dummyChannels = getDummyChannels();

        dummyChannels.entrySet()
                .forEach(entry-> jmxConfig
                        .createChannel(entry.getKey(),
                                entry.getValue().getHost(),
                                String.valueOf(entry.getValue().getPort())));

        jmxConfig.deleteChannel("aurora3");

        String statusReport = jmxConfig.getChannelsStatus();

        for (int i = 0; i < 3 ; i++) {
            //do assert.
            assertTrue(statusReport.contains("aurora"+i+" : CONNECTING"));
        }

        assertFalse(statusReport.contains("aurora3"));

        assertTrue(publishers.getPublishersList().get(0).equals("aurora"));
    }

    private Map<String, ChannelProperty> getDummyChannels(){
        Map<String, ChannelProperty> channelPropertyMap = new HashMap<>();
        for (int i = 0; i < 4 ; i++) {
            ChannelProperty property = new ChannelProperty();
            property.setHost("localhost");
            property.setPort(300 + i);
            channelPropertyMap.put("aurora"+i,property);
        }

        return channelPropertyMap;
    }


    private void createFakeReplyingServer() throws IOException {
        grpcCleanup.register(InProcessServerBuilder
                .forName(receiverChannelName)
                .directExecutor()
                .addService(this.fakeReplyingServiceForReceiver())
                .build()
                .start());

    }

    private void createFakeOrderBookServer() throws IOException {
        grpcCleanup.register(InProcessServerBuilder
                .forName(receiverChannelName)
                .directExecutor()
                .addService(this.fakeReplyingOrderBookService())
                .build()
                .start());

    }

    private void createFakeMarketServer() throws IOException {
        grpcCleanup.register(InProcessServerBuilder
                .forName(receiverChannelName)
                .directExecutor()
                .addService(this.fakeReplyingMarketService())
                .build()
                .start());

    }

    MarketServiceGrpc.MarketServiceImplBase fakeReplyingMarketService(){
        return new MarketServiceGrpc.MarketServiceImplBase() {
            @Override
            public void subscribeForItem(MarketDataRequest request, StreamObserver<TickResponse> responseObserver) {
                for (int i = 0; i < 5; i++) {
                    TickResponse response = TickResponse.newBuilder()
                            .setGoodName(request.getGoodName())
                            .build();
                    responseObserver.onNext(response);
                }
                responseObserver.onCompleted();
            }
        };
    }


    OrderBookServiceGrpc.OrderBookServiceImplBase fakeReplyingOrderBookService(){
        return new OrderBookServiceGrpc.OrderBookServiceImplBase() {
            @Override
            public void getOrderBookItemLayers(ItemOrderBookRequest request, StreamObserver<ItemOrderBookResponse> responseObserver) {
                responseObserver.onNext(ItemOrderBookResponse
                        .newBuilder()
                        .setItemName(request.getItemName())
                        .build());

                responseObserver.onCompleted();
            }

            @Override
            public void announceItemInterest(InterestsRequest request, StreamObserver<InterestsResponse> responseObserver) {
               responseObserver.onNext(InterestsResponse.newBuilder().build());
               responseObserver.onCompleted();
            }

            @Override
            public void cancelItemSubscription(CancelSubscriptionRequest request, StreamObserver<CancelSubscriptionResponse> responseObserver) {
                responseObserver.onNext(CancelSubscriptionResponse.newBuilder().build());
                responseObserver.onCompleted();
            }
        };
    }



    AuroraServiceGrpc.AuroraServiceImplBase fakeReplyingServiceForReceiver() {
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
                for (int i = 0; i < 5; i++) {
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

    private void createFakeErrorServer() throws IOException {
        grpcCleanup.register(InProcessServerBuilder
                .forName(receiverChannelName)
                .directExecutor()
                .addService(this.fakeErrorServiceForReceiver())
                .build()
                .start());

    }

    AuroraServiceGrpc.AuroraServiceImplBase fakeErrorServiceForReceiver() {
        return new AuroraServiceGrpc.AuroraServiceImplBase() {
            @Override
            public void request(Aurora.AuroraRequest request, StreamObserver<Aurora.AuroraResponse> responseObserver) {
                responseObserver.onError(Status.ABORTED
                        .withDescription("Aborting request.")
                        .asException());
            }

            @Override
            public void subscribe(Aurora.AuroraRequest request, StreamObserver<Aurora.AuroraResponse> responseObserver) {

                for (int i = 0; i < 5; i++) {
                    if (i == 3) {
                        responseObserver.onError(Status.ABORTED
                                .withDescription("Aborting subscribe.")
                                .asException());
                        break;
                    }
                    Aurora.AuroraResponse response = Aurora.AuroraResponse.newBuilder().setMessage(Any.pack(request)).build();
                    responseObserver.onNext(response);
                }
            }
        };
    }


    private void createFakeSender() throws IOException {
        grpcCleanup.register(InProcessServerBuilder.forName(senderName).directExecutor()
                .addService(new AuroraServiceGrpc.AuroraServiceImplBase() {
                    @Override
                    public void request(Aurora.AuroraRequest request, StreamObserver<Aurora.AuroraResponse> responseObserver) {
                        auroraService.request(request, responseObserver);
                    }

                    @Override
                    public void subscribe(Aurora.AuroraRequest request, StreamObserver<Aurora.AuroraResponse> responseObserver) {
                        auroraService.subscribe(request, responseObserver);
                    }
                })
                .build()
                .start());
    }

    private Aurora.AuroraRequest getAuroraRequest() {
        Aurora.AuroraRequest request = Aurora.AuroraRequest.newBuilder()
                .setTopic("aurora" + "/test")
                .setClientId("integration test for requests")
                .build();
        return request;
    }
}
