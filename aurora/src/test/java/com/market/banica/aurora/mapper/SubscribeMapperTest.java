package com.market.banica.aurora.mapper;

import com.aurora.Aurora;
import com.aurora.AuroraServiceGrpc;
import com.google.protobuf.Any;
import com.market.MarketDataRequest;
import com.market.MarketServiceGrpc;
import com.market.TickResponse;
import com.market.banica.aurora.config.ChannelManager;
import com.market.banica.aurora.config.StubManager;
import com.market.banica.aurora.observer.MarketTickObserver;
import com.orderbook.OrderBookServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.rmi.NoSuchObjectException;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscribeMapperTest {


    private final ManagedChannel dummyManagedChannel = ManagedChannelBuilder
            .forAddress("localhost", 1010)
            .usePlaintext()
            .build();

    private static final Aurora.AuroraRequest MARKET_REQUEST = Aurora.AuroraRequest.newBuilder().setClientId("1").setTopic("market/eggs/10").build();
    private static final Aurora.AuroraRequest AURORA_REQUEST = Aurora.AuroraRequest.newBuilder().setTopic("aurora/eggs/10").build();
    private static final Aurora.AuroraRequest ORDERBOOK_REQUEST = Aurora.AuroraRequest.newBuilder().setTopic("orderbook/eggs/10").build();
    private static final Aurora.AuroraRequest ORDERBOOK_SUBSCRIBE_REQUEST = Aurora.AuroraRequest.newBuilder().setTopic("orderbook/eggs=subscribe").build();
    private static final Aurora.AuroraRequest ORDERBOOK_UNSUBSCRIBE_REQUEST = Aurora.AuroraRequest.newBuilder().setTopic("orderbook/eggs=unsubscribe").build();
    private static final Aurora.AuroraRequest INVALID_REQUEST = Aurora.AuroraRequest.newBuilder().setTopic("market/banica").build();

    private String auroraReceiverName;
    private ManagedChannel auroraReceiverChannel;

    private String marketReceiverName;
    private ManagedChannel marketReceiverChannel;

    private MarketServiceGrpc.MarketServiceStub marketStub;
    private AuroraServiceGrpc.AuroraServiceBlockingStub auroraBlockingStub;


    private final StreamObserver<Aurora.AuroraResponse> responseObserver = mock(StreamObserver.class);


    @Rule
    public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    @Mock
    private ChannelManager channelManager;

    @Mock
    private StubManager stubManager;

    @InjectMocks
    @Spy
    private SubscribeMapper subscribeMapper;

    @BeforeEach
    public void setUp() {
        auroraReceiverName = InProcessServerBuilder.generateName();
        auroraReceiverChannel = InProcessChannelBuilder
                .forName(auroraReceiverName)
                .executor(Executors.newSingleThreadExecutor()).build();

        auroraBlockingStub = AuroraServiceGrpc.newBlockingStub(auroraReceiverChannel);

        marketReceiverName = InProcessServerBuilder.generateName();
        marketReceiverChannel = InProcessChannelBuilder
                .forName(marketReceiverName)
                .executor(Executors.newSingleThreadExecutor()).build();

        marketStub = MarketServiceGrpc.newStub(marketReceiverChannel);
    }

    @Test
    void renderSubscribeWithRequestForServiceWithNonExistentChannelInvokesOnError() {
        when(channelManager.getAllChannelsContainingPrefix(any())).thenReturn(Collections.emptyList());
        subscribeMapper.renderSubscribe(INVALID_REQUEST, responseObserver);
        verify(responseObserver, times(1)).onError(any());
    }

    @Test
    void renderSubscribeWithRequestForMarketService() throws IOException {
        when(channelManager.getAllChannelsContainingPrefix(any())).thenReturn(Collections.singletonList(dummyManagedChannel));
        when(stubManager.getMarketStub(any())).thenReturn(marketStub);

        subscribeMapper.renderSubscribe(MARKET_REQUEST, responseObserver);

        verify(stubManager,times(1)).getMarketStub(any());
        verify(responseObserver, atLeastOnce()).onCompleted();
    }

    private void createFakeMarketReplyingServer() throws IOException {
        grpcCleanup.register(InProcessServerBuilder
                .forName(marketReceiverName)
                .directExecutor()
                .addService(this.fakeReceivingMarketService())
                .build()
                .start());
    }

    private void createFakeAuroraReplyingServer() throws IOException {
        grpcCleanup.register(InProcessServerBuilder
                .forName(auroraReceiverName)
                .directExecutor()
                .addService(this.fakeReceivingAuroraService())
                .build()
                .start());
    }

    private MarketServiceGrpc.MarketServiceImplBase fakeReceivingMarketService() {
        return new MarketServiceGrpc.MarketServiceImplBase() {
            @Override
            public void subscribeForItem(MarketDataRequest request, StreamObserver<TickResponse> responseObserver) {

                TickResponse response = TickResponse
                        .newBuilder()
                        .setGoodName("eggs")
                        .build();

                responseObserver.onNext(response);

                responseObserver.onCompleted();
            }
        };
    }


    private AuroraServiceGrpc.AuroraServiceImplBase fakeReceivingAuroraService() {
        return new AuroraServiceGrpc.AuroraServiceImplBase() {
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
}