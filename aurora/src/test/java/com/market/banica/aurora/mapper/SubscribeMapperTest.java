package com.market.banica.aurora.mapper;

import com.aurora.Aurora;
import com.aurora.AuroraServiceGrpc;
import com.market.MarketServiceGrpc;
import com.market.banica.aurora.config.ChannelManager;
import com.market.banica.aurora.config.StubManager;
import com.market.banica.aurora.util.FakeStubsGenerator;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.Rule;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.Executors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscribeMapperTest {

    private static final Aurora.AuroraRequest MARKET_REQUEST = Aurora.AuroraRequest.newBuilder().setClientId("1").setTopic("market/eggs/10").build();
    private static final Aurora.AuroraRequest AURORA_REQUEST = Aurora.AuroraRequest.newBuilder().setTopic("aurora/eggs/10").build();
    private static final Aurora.AuroraRequest ORDERBOOK_REQUEST = Aurora.AuroraRequest.newBuilder().setTopic("orderbook/eggs/10").build();
    private static final Aurora.AuroraRequest INVALID_REQUEST = Aurora.AuroraRequest.newBuilder().setTopic("invalid/request").build();

    private static final ManagedChannel DUMMY_MANAGED_CHANNEL = ManagedChannelBuilder
            .forAddress("localhost", 1010)
            .usePlaintext()
            .build();

    private static final String AURORA_SERVER_NAME = "auroraServer";
    private static final String MARKET_SERVER_NAME = "marketServer";

    private static final ManagedChannel AURORA_SERVER_CHANNEL = InProcessChannelBuilder
            .forName(AURORA_SERVER_NAME)
            .executor(Executors.newSingleThreadExecutor()).build();
    private static final ManagedChannel MARKET_SERVER_CHANNEL = InProcessChannelBuilder
            .forName(MARKET_SERVER_NAME)
            .executor(Executors.newSingleThreadExecutor()).build();

    private final MarketServiceGrpc.MarketServiceStub marketStub = MarketServiceGrpc.newStub(MARKET_SERVER_CHANNEL);
    private final AuroraServiceGrpc.AuroraServiceStub auroraStub = AuroraServiceGrpc.newStub(AURORA_SERVER_CHANNEL);

    private final StreamObserver<Aurora.AuroraResponse> responseObserver = mock(StreamObserver.class);

    @Rule
    public static GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    @Mock
    private ChannelManager channelManager;

    @Mock
    private StubManager stubManager;

    @InjectMocks
    @Spy
    private SubscribeMapper subscribeMapper;

    @BeforeAll
    static void setUp() throws IOException {
        FakeStubsGenerator.addChannel("aurora", AURORA_SERVER_CHANNEL);
        FakeStubsGenerator.addChannel("market", MARKET_SERVER_CHANNEL);
        FakeStubsGenerator.addChannel("dummy", DUMMY_MANAGED_CHANNEL);

        FakeStubsGenerator.createFakeServer(AURORA_SERVER_NAME, grpcCleanup, AURORA_SERVER_CHANNEL);
        FakeStubsGenerator.createFakeServer(MARKET_SERVER_NAME, grpcCleanup, MARKET_SERVER_CHANNEL);
    }

    @AfterAll
    public static void shutDownChannels() {
        FakeStubsGenerator.shutDownAllChannels();
    }

    @Test
    void renderSubscribeWithRequestForServiceWithNonExistentChannelInvokesOnError() {
        when(channelManager.getAllChannelsContainingPrefix(any())).thenReturn(Collections.emptyList());
        subscribeMapper.renderSubscribe(INVALID_REQUEST, responseObserver);
        verify(responseObserver, times(1)).onError(any());
    }

    @Test
    void renderSubscribeWithRequestForOrderBookInvokesOnError() {
        when(channelManager.getAllChannelsContainingPrefix(any())).thenReturn(Collections.singletonList(DUMMY_MANAGED_CHANNEL));
        subscribeMapper.renderSubscribe(ORDERBOOK_REQUEST, responseObserver);
        verify(responseObserver, times(1)).onError(any());
    }

    @Test
    void renderSubscribeWithRequestForUnsupportedServiceInvokesOnError() {
        when(channelManager.getAllChannelsContainingPrefix(any())).thenReturn(Collections.singletonList(DUMMY_MANAGED_CHANNEL));
        subscribeMapper.renderSubscribe(INVALID_REQUEST, responseObserver);
        verify(responseObserver, times(1)).onError(any());
    }

    @Test
    void renderSubscribeWithRequestForMarketServiceSubscribesResponseObserverAndCallsOnNextAndOnCompleted() throws InterruptedException {
        //Arrange
        when(channelManager.getAllChannelsContainingPrefix(any())).thenReturn(Collections.singletonList(DUMMY_MANAGED_CHANNEL));
        when(stubManager.getMarketStub(any())).thenReturn(marketStub);

        //Act
        subscribeMapper.renderSubscribe(MARKET_REQUEST, responseObserver);

        Thread.sleep(1000);

        //Assert
        verify(stubManager, times(1)).getMarketStub(any());
        verify(responseObserver, times(1)).onNext(any());
        verify(responseObserver, times(1)).onCompleted();
    }

    @Test
    void renderSubscribeWithRequestForAuroraServiceSubscribesResponseObserverAndCallsOnNextAndOnCompleted() throws InterruptedException {
        //Arrange
        when(channelManager.getAllChannelsContainingPrefix(any())).thenReturn(Collections.singletonList(DUMMY_MANAGED_CHANNEL));
        when(stubManager.getAuroraStub(any())).thenReturn(auroraStub);

        //Act
        subscribeMapper.renderSubscribe(AURORA_REQUEST, responseObserver);

        Thread.sleep(1000);

        //Assert
        verify(stubManager, times(1)).getAuroraStub(any());
        verify(responseObserver, times(3)).onNext(any());
        verify(responseObserver, times(1)).onCompleted();
    }
}