package com.market.banica.aurora.observer;

import com.aurora.Aurora;
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
class AuroraObserverTest {

    private static final Aurora.AuroraRequest AURORA_REQUEST_BANICA = Aurora.AuroraRequest.newBuilder().setTopic("market/banica").setClientId("orderBook").build();

    private static final Aurora.AuroraResponse AURORA_RESPONSE_TO_FORWARD = Aurora.AuroraResponse.newBuilder().build();

    private static final Throwable THROWABLE = new Throwable("Error message.");

    private final AtomicInteger activeStreamsCounter = new AtomicInteger(1);

    private final StreamObserver<Aurora.AuroraResponse> forwardResponse = mock(StreamObserver.class);

    @InjectMocks
    private AuroraObserver auroraObserver;

    @BeforeEach
    public void setUp() {
        auroraObserver = new AuroraObserver(AURORA_REQUEST_BANICA, forwardResponse, activeStreamsCounter);
    }

    @Test
    void onNextForwardsResponseToResponseObserver() {
        auroraObserver.onNext(AURORA_RESPONSE_TO_FORWARD);
        verify(forwardResponse, times(1)).onNext(AURORA_RESPONSE_TO_FORWARD);
    }

    @Test
    void onErrorDecrementsOpenStreams() {
        assertEquals(1, activeStreamsCounter.get());
        auroraObserver.onError(THROWABLE);

        assertEquals(0, activeStreamsCounter.get());
    }

    @Test
    void onErrorWhenNoOpenStreamsThenInvokesResponseObserversOnComplete() {
        assertEquals(1, activeStreamsCounter.get());
        auroraObserver.onError(THROWABLE);

        assertEquals(0, activeStreamsCounter.get());
        verify(forwardResponse, times(1)).onCompleted();
    }

    @Test
    void onCompleteDecrementsOpenStreams() {
        assertEquals(1, activeStreamsCounter.get());
        auroraObserver.onCompleted();

        assertEquals(0, activeStreamsCounter.get());
    }

    @Test
    void onCompletedWhenNoOpenStreamsThenInvokesResponseObserversOnComplete() {
        assertEquals(1, activeStreamsCounter.get());
        auroraObserver.onCompleted();

        assertEquals(0, activeStreamsCounter.get());
        verify(forwardResponse, times(1)).onCompleted();
    }
}