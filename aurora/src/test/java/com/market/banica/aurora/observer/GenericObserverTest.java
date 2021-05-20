package com.market.banica.aurora.observer;

import com.aurora.Aurora;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.Any;
import com.market.MarketDataRequest;
import com.market.MarketServiceGrpc;
import com.market.TickResponse;
import com.market.banica.aurora.backpressure.BackPressureManager;
import com.market.banica.aurora.util.FakeServerGenerator;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.Rule;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GenericObserverTest {
    private static final Aurora.AuroraResponse AURORA_RESPONSE_TO_FORWARD = Aurora.AuroraResponse
            .newBuilder().setMessage(Any.pack(TickResponse.newBuilder().setGoodName("banica").build())).build();

    private static final String MARKET_SERVER_NAME = "marketServer";
    private static final String CLIENT_ID = "aurora";
    private static final String ITEM_NAME = "banica";
    private static final String ORDER_BOOK_GRPC_IDENTIFIER = "9090";

    private static final ManagedChannel DUMMY_MANAGED_CHANNEL = ManagedChannelBuilder
            .forAddress("localhost", 1010)
            .usePlaintext()
            .build();

    private static final ManagedChannel MARKET_SERVER_CHANNEL = InProcessChannelBuilder
            .forName(MARKET_SERVER_NAME)
            .executor(MoreExecutors.directExecutor()).build();

    private final MarketServiceGrpc.MarketServiceStub marketStub = MarketServiceGrpc.newStub(MARKET_SERVER_CHANNEL);

    private static final Throwable THROWABLE = new Throwable("Error message.");

    private final AtomicInteger activeStreamsCounter = new AtomicInteger(1);

    private final StreamObserver<Aurora.AuroraResponse> forwardResponse = mock(StreamObserver.class);

    @Rule
    public static GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    @Mock
    private BackPressureManager backPressureManager;


    @InjectMocks
    private GenericObserver<MarketDataRequest,TickResponse> marketTickObserver;

    @BeforeEach
    public void setUp() throws IOException {
        FakeServerGenerator.createFakeServer(MARKET_SERVER_NAME, grpcCleanup, MARKET_SERVER_CHANNEL);

        FakeServerGenerator.addChannel("marketServerChannel", MARKET_SERVER_CHANNEL);
        FakeServerGenerator.addChannel("dummyChannel", DUMMY_MANAGED_CHANNEL);

        MarketDataRequest marketDataRequest = MarketDataRequest.newBuilder()
                .setClientId(CLIENT_ID)
                .setGoodName(ITEM_NAME)
                .build();

        marketTickObserver = new GenericObserver(CLIENT_ID, forwardResponse, activeStreamsCounter,
                "TEST", ITEM_NAME, marketDataRequest, backPressureManager, ORDER_BOOK_GRPC_IDENTIFIER);
    }

    @AfterAll
    public static void shutDownChannels() {
        FakeServerGenerator.shutDownAllChannels();
    }

    @Test
    void onNextForwardsResponseToResponseObserver() {
        marketStub.subscribeForItem(marketTickObserver);
        verify(forwardResponse, times(1)).onNext(AURORA_RESPONSE_TO_FORWARD);
    }

    @Test
    void onErrorDecrementsOpenStreams() {
        assertEquals(1, activeStreamsCounter.get());
        marketTickObserver.onError(THROWABLE);

        assertEquals(0, activeStreamsCounter.get());
    }

    @Test
    void onErrorWhenNoOpenStreamsThenInvokesForwardResponseOnComplete() {
        assertEquals(1, activeStreamsCounter.get());
        marketTickObserver.onError(THROWABLE);

        assertEquals(0, activeStreamsCounter.get());
        verify(forwardResponse, times(1)).onCompleted();
    }

    @Test
    void onCompleteDecrementsOpenStreams() {
        assertEquals(1, activeStreamsCounter.get());
        marketTickObserver.onCompleted();

        assertEquals(0, activeStreamsCounter.get());
    }

    @Test
    void onCompletedWhenNoOpenStreamsThenInvokesForwardResponseOnComplete() {
        assertEquals(1, activeStreamsCounter.get());
        marketTickObserver.onCompleted();

        assertEquals(0, activeStreamsCounter.get());
        verify(forwardResponse, times(1)).onCompleted();
    }
}