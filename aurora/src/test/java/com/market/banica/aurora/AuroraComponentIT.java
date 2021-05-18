package com.market.banica.aurora;

import com.aurora.Aurora;
import com.aurora.AuroraServiceGrpc;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.market.MarketDataRequest;
import com.market.MarketServiceGrpc;
import com.market.TickResponse;
import com.market.banica.aurora.config.ChannelManager;
import com.market.banica.aurora.config.GrpcClassProvider;
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
import lombok.SneakyThrows;
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
class AuroraComponentIT {


    @Autowired
    private ChannelManager channelManager;

    @Autowired
    private Publishers publishers;

    @Autowired
    GrpcClassProvider provider;

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
    private static String grpcClassNamesUrl;

    private static final Set<String> channelNames = new HashSet<>();

    private static String currentPublisher;


    @BeforeAll
    public static void getFilePath(@Value("${aurora.channels.file.name}") String channels, @Value("${aurora.channels.publishers}") String publishersFileName,
                                   @Value("${aurora.grpc.classes.filenames}") String grpcClassNamesFiles) {
        channelsBackupUrl = channels;
        publishersBackupUrl = publishersFileName;
        grpcClassNamesUrl = grpcClassNamesFiles;
    }


    @AfterAll
    public static void cleanUp() throws IOException {
        ApplicationDirectoryUtil.getConfigFile(channelsBackupUrl).delete();
        ApplicationDirectoryUtil.getConfigFile(publishersBackupUrl).delete();
        ApplicationDirectoryUtil.getConfigFile(grpcClassNamesUrl).delete();
    }


    @BeforeEach
    public void setup() throws ClassNotFoundException {
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

        provider.addClass("aurora","com.aurora.AuroraServiceGrpc");
        provider.addClass("orderbook","com.orderbook.OrderBookServiceGrpc");
        provider.addClass("market","com.market.MarketServiceGrpc");

    }

    @AfterEach
    public void tearDown()  {
        publishers.deletePublisher(currentPublisher);
        channelManager.removeChannel(currentPublisher);
        provider.removeClass("aurora");
        provider.removeClass("orderbook");
        provider.removeClass("market");
    }


    @Test
    void request_Should_ForwardToReceiverResponse_When_ReceiverIsRegistered() throws IOException {
        //Arrange
        createFakeReplyingServer();
        createFakeSender();

        grpcCleanup.register(senderChannel);
        grpcCleanup.register(receiverChannel);

        currentPublisher = "aurora";

        publishers.addPublisher(currentPublisher);
        receiverChannel.getState(true);
        channelManager.addChannel(currentPublisher, receiverChannel);

        Aurora.AuroraRequest request = getAuroraRequest();

        //Act
        Aurora.AuroraResponse response = AuroraServiceGrpc
                .newBlockingStub(senderChannel)
                .request(request);

        //Assert
        assertEquals(request, response.getMessage().unpack(Aurora.AuroraRequest.class));

    }

    @Test
    void request_Should_ForwardToReceiverError_When_ReceiverIsNotRegistered() throws IOException {
        //Arrange

        createFakeErrorServer();
        createFakeSender();

        grpcCleanup.register(senderChannel);
        grpcCleanup.register(receiverChannel);

        currentPublisher = "aurora";

        publishers.addPublisher(currentPublisher);
        receiverChannel.getState(true);
        channelManager.addChannel(currentPublisher, receiverChannel);

        Aurora.AuroraRequest request = getAuroraRequest();

        //Act & Assert
        assertThrows(StatusRuntimeException.class, () -> AuroraServiceGrpc
                .newBlockingStub(senderChannel)
                .request(request));

    }

    @Test
    void request_Should_ForwardToReceiverError_When_NoSupportedMapping() throws IOException {
        //Arrange
        currentPublisher = "market";
        publishers.addPublisher(currentPublisher);

        createFakeMarketServer();
        createFakeSender();

        grpcCleanup.register(senderChannel);
        grpcCleanup.register(receiverChannel);

        receiverChannel.getState(true);
        channelManager.addChannel(currentPublisher, receiverChannel);

        Aurora.AuroraRequest request = Aurora.AuroraRequest.newBuilder().setTopic("market/something").build();

        //Act & Assert
        assertThrows(StatusRuntimeException.class, () -> AuroraServiceGrpc
                .newBlockingStub(senderChannel)
                .request(request));

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

        channelManager.addChannel(currentPublisher, receiverChannel);
        publishers.addPublisher(currentPublisher);
    }

    @Test
    void ItemOrderBookRequest_Should_ForwardToOrderbookResponse_When_OrderbookIsRegistered() throws IOException {
        //Arrange
        currentPublisher = "orderbook";
        publishers.addPublisher(currentPublisher);
        createFakeOrderBookServer();
        createFakeSender();

        grpcCleanup.register(senderChannel);
        grpcCleanup.register(receiverChannel);

        receiverChannel.getState(true);
        channelManager.addChannel(currentPublisher, receiverChannel);

        Aurora.AuroraRequest itemOrderBookRequest = Aurora.AuroraRequest.newBuilder()
                .setClientId("fake calculator item orderbook request")
                .setTopic("orderbook/eggs/15").build();

        //Act
        Aurora.AuroraResponse response = AuroraServiceGrpc
                .newBlockingStub(senderChannel)
                .request(itemOrderBookRequest);

        //Assert
        assertTrue(response.getMessage().is(ItemOrderBookResponse.class));
        assertEquals("eggs", response.getMessage().unpack(ItemOrderBookResponse.class).getItemName());

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

    }

    @Test
    void subscribe_Should_ForwardToReceiverResponses_When_ReceiverIsRegistered() throws IOException, InterruptedException {
        //Arrange
        currentPublisher = "aurora-test";
        publishers.addPublisher(currentPublisher);

        createFakeReplyingServer();
        createFakeSender();


        grpcCleanup.register(senderChannel);
        grpcCleanup.register(receiverChannel);

        receiverChannel.getState(true);
        channelManager.addChannel(currentPublisher, receiverChannel);

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
                    @SneakyThrows
                    @Override
                    public void onNext(Aurora.AuroraResponse response) {
                        Aurora.AuroraResponse unpack = response.getMessage().unpack(Aurora.AuroraResponse.class);
                        receivedResponses.add(unpack);
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
    }

    @Test
    void subscribeToMarket_Should_ForwardToReceiverResponse_When_MarketIsRegistered() throws IOException, InterruptedException {
        //Arrange
        currentPublisher = "market";
        publishers.addPublisher(currentPublisher);
        createFakeMarketServer();

        createFakeSender();


        grpcCleanup.register(senderChannel);
        grpcCleanup.register(receiverChannel);

        receiverChannel.getState(true);
        channelManager.addChannel(currentPublisher, receiverChannel);

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
    }

    @Test
    void subscribeToOrderbook_Should_ForwardToReceiverError_When_NoSupportedMapping() throws IOException, InterruptedException {
        //Arrange
        currentPublisher = "orderbook";
        publishers.addPublisher(currentPublisher);
        createFakeOrderBookServer();
        createFakeSender();

        grpcCleanup.register(senderChannel);
        grpcCleanup.register(receiverChannel);

        receiverChannel.getState(true);
        channelManager.addChannel(currentPublisher, receiverChannel);

        Aurora.AuroraRequest request = Aurora.AuroraRequest.newBuilder().setTopic("orderbook/something").build();

        final boolean[] inError = {false};

        CountDownLatch latch = new CountDownLatch(1);


        //Act
        AuroraServiceGrpc
                .newStub(senderChannel)
                .subscribe(request, new StreamObserver<Aurora.AuroraResponse>() {
                    @Override
                    public void onNext(Aurora.AuroraResponse response) {

                    }

                    @Override
                    public void onError(Throwable throwable) {
                        inError[0] = true;
                        latch.countDown();
                    }

                    @Override
                    public void onCompleted() {
                        latch.countDown();
                    }
                });


        //Assert
        latch.await();
        assertTrue(inError[0]);
    }


    @Test
    void subscribeToMarket_Should_ForwardToReceiverLessResponses_When_MarketSendsErrorDuringStream() throws IOException, InterruptedException {
        //Arrange
        currentPublisher = "market";
        publishers.addPublisher(currentPublisher);
        createFakeMarketErrorServer();
        createFakeSender();


        grpcCleanup.register(senderChannel);
        grpcCleanup.register(receiverChannel);

        receiverChannel.getState(true);
        channelManager.addChannel(currentPublisher, receiverChannel);

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
        assertNotEquals(expectedResponses, unpackedResponses);
        assertEquals(3, unpackedResponses.size());
    }

    @Test
    void subscribe_Should_ForwardToReceiverLessResponses_When_ReceiverSendsErrorDuringStream() throws IOException, InterruptedException {
        //Arrange
        currentPublisher = "aurora";
        publishers.addPublisher(currentPublisher);

        createFakeErrorServer();
        createFakeSender();


        grpcCleanup.register(senderChannel);
        grpcCleanup.register(receiverChannel);

        receiverChannel.getState(true);
        channelManager.addChannel(currentPublisher, receiverChannel);

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
        assertEquals(3, receivedResponses.size());
    }

    @Test
    void subscribe_Should_ForwardToReceiverNoResponses_When_NoExistingChannel() throws IOException, InterruptedException {
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
        assertEquals(0, receivedResponses.size());

        publishers.addPublisher(currentPublisher);
        channelManager.addChannel(currentPublisher, receiverChannel);
    }

    @Test
    void jmx_ChannelManager_Publishers_FlowTest() {
        currentPublisher = "aurora";
        publishers.addPublisher(currentPublisher);
        Map<String, ChannelProperty> dummyChannels = getDummyChannels();

        dummyChannels.forEach((key, value) -> jmxConfig
                .createChannel(key,
                        value.getHost(),
                        String.valueOf(value.getPort())));


        jmxConfig.editChannel("aurora3", "localhost", "2343");
        jmxConfig.deleteChannel("aurora3");

        String statusReport = jmxConfig.getChannelsStatus();

        for (int i = 0; i < 3; i++) {
            //do assert.
            assertTrue(statusReport.contains("aurora" + i));
            jmxConfig.deleteChannel("aurora" + i);
        }

        assertFalse(statusReport.contains("aurora3"));

        assertEquals("aurora", publishers.getPublishersList().get(0));
    }

    private Map<String, ChannelProperty> getDummyChannels() {
        Map<String, ChannelProperty> channelPropertyMap = new HashMap<>();
        for (int i = 0; i < 4; i++) {
            ChannelProperty property = new ChannelProperty();
            property.setHost("localhost");
            property.setPort(300 + i);
            channelPropertyMap.put("aurora" + i, property);
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

    private void createFakeMarketErrorServer() throws IOException {
        grpcCleanup.register(InProcessServerBuilder
                .forName(receiverChannelName)
                .directExecutor()
                .addService(this.fakeErrorReplyingMarketService())
                .build()
                .start());

    }

    private MarketServiceGrpc.MarketServiceImplBase fakeReplyingMarketService() {
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

    MarketServiceGrpc.MarketServiceImplBase fakeErrorReplyingMarketService() {
        return new MarketServiceGrpc.MarketServiceImplBase() {
            @Override
            public void subscribeForItem(MarketDataRequest request, StreamObserver<TickResponse> responseObserver) {
                for (int i = 0; i < 5; i++) {
                    if (i == 3) {
                        responseObserver.onError(Status.ABORTED
                                .withDescription("Aborting subscribe.")
                                .asException());
                        break;
                    }
                    TickResponse response = TickResponse.newBuilder()
                            .setGoodName(request.getGoodName())
                            .build();
                    responseObserver.onNext(response);
                }
            }
        };
    }


    OrderBookServiceGrpc.OrderBookServiceImplBase fakeReplyingOrderBookService() {
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
        return Aurora.AuroraRequest.newBuilder()
                .setTopic("aurora" + "/test")
                .setClientId("integration test for requests")
                .build();
    }
}
