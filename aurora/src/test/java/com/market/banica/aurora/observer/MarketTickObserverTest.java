package com.market.banica.aurora.observer;

import com.aurora.Aurora;
import com.google.protobuf.Any;
import com.market.TickResponse;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MarketTickObserverTest {
    private static final Aurora.AuroraResponse AURORA_RESPONSE_TO_FORWARD = Aurora.AuroraResponse
            .newBuilder().setMessage(Any.pack(TickResponse.newBuilder().setGoodName("banica").build())).build();

    private static final Throwable THROWABLE = new Throwable("Error message.");

    private final AtomicInteger activeStreamsCounter = new AtomicInteger(1);

    private final StreamObserver<Aurora.AuroraResponse> forwardResponse = mock(StreamObserver.class);

    private final String CLIENT_ID = "aurora";

    @InjectMocks
    private MarketTickObserver marketTickObserver;

    @BeforeEach
    public void setUp() {
        marketTickObserver = new MarketTickObserver(CLIENT_ID, forwardResponse, activeStreamsCounter,"TEST","banica");
    }

    @Test
    void onNextForwardsResponseToResponseObserver() throws InterruptedException {
        marketTickObserver.onNext(TickResponse.newBuilder().setGoodName("banica").build());
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