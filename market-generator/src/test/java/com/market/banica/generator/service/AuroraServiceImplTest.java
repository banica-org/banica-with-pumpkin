package com.market.banica.generator.service;

import com.aurora.Aurora;
import com.google.protobuf.Any;
import com.market.TickResponse;
import com.market.banica.generator.service.grpc.AuroraService;
import io.grpc.Status;
import io.grpc.stub.ServerCallStreamObserver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuroraServiceImplTest {

    @Mock
    private MarketSubscriptionManager marketSubscriptionServiceImpl;

    @Mock
    private MarketState marketState;

    @InjectMocks
    private AuroraService auroraService;

    private final String TOPIC_BANICA = "market/banica";

    private final Aurora.AuroraRequest AURORA_REQUEST_BANICA = Aurora.AuroraRequest.newBuilder()
            .setTopic(TOPIC_BANICA)
            .build();

    @SuppressWarnings("unchecked")
    private final ServerCallStreamObserver<Aurora.AuroraResponse> subscriber = mock(ServerCallStreamObserver.class);

    @Test
    void subscribe_ValidRequest_BootstrapAndAddSubscriber() {

        TickResponse tick1 = TickResponse.newBuilder()
                .setGoodName("firstTick")
                .build();
        TickResponse tick2 = TickResponse.newBuilder()
                .setGoodName("secondTick")
                .build();

        List<TickResponse> ticks = Arrays.asList(tick1, tick2);

        String[] topic = AURORA_REQUEST_BANICA.getTopic().split("/");

        Aurora.AuroraResponse response1 = Aurora.AuroraResponse.newBuilder().setMessage(Any.pack(tick1)).build();
        Aurora.AuroraResponse response2 = Aurora.AuroraResponse.newBuilder().setMessage(Any.pack(tick2)).build();

        when(marketSubscriptionServiceImpl.getGoodNameFromRequest(AURORA_REQUEST_BANICA)).thenReturn(topic[1]);
        when(marketState.generateMarketTicks(topic[1])).thenReturn(ticks);
        when(marketState.generateMarketTicks(topic[1])).thenReturn(ticks);
        when(marketSubscriptionServiceImpl.convertTickResponseToAuroraResponse(tick1)).thenReturn(response1);
        when(marketSubscriptionServiceImpl.convertTickResponseToAuroraResponse(tick2)).thenReturn(response2);


        auroraService.subscribe(AURORA_REQUEST_BANICA, subscriber);


        verify(marketSubscriptionServiceImpl, times(1))
                .subscribe(AURORA_REQUEST_BANICA, subscriber);
        verify(marketState, times(1)).generateMarketTicks("banica");
        verify(marketSubscriptionServiceImpl, times(1))
                .convertTickResponseToAuroraResponse(tick1);
        verify(marketSubscriptionServiceImpl, times(1))
                .convertTickResponseToAuroraResponse(tick2);
        verify(subscriber, times(1)).onNext(response1);
        verify(subscriber, times(1)).onNext(response2);

    }

    @Test
    void subscribe_StreamCancelled_BootstrapFailAndNoAddedSubscriber() {

        TickResponse tick1 = TickResponse.newBuilder()
                .setGoodName("firstTick")
                .build();
        TickResponse tick2 = TickResponse.newBuilder()
                .setGoodName("secondTick")
                .build();

        List<TickResponse> ticks = Arrays.asList(tick1, tick2);

        String[] topic = AURORA_REQUEST_BANICA.getTopic().split("/");

        Aurora.AuroraResponse response1 = Aurora.AuroraResponse.newBuilder().setMessage(Any.pack(tick1)).build();
        Aurora.AuroraResponse response2 = Aurora.AuroraResponse.newBuilder().setMessage(Any.pack(tick2)).build();

        when(marketSubscriptionServiceImpl.getGoodNameFromRequest(AURORA_REQUEST_BANICA)).thenReturn(topic[1]);
        when(marketState.generateMarketTicks(topic[1])).thenReturn(ticks);
        when(marketState.generateMarketTicks(topic[1])).thenReturn(ticks);

        when(subscriber.isCancelled()).thenReturn(true);


        auroraService.subscribe(AURORA_REQUEST_BANICA, subscriber);


        verify(marketSubscriptionServiceImpl, times(0))
                .subscribe(AURORA_REQUEST_BANICA, subscriber);
        verify(marketState, times(1)).generateMarketTicks("banica");
        verify(marketSubscriptionServiceImpl, times(0))
                .convertTickResponseToAuroraResponse(tick1);
        verify(marketSubscriptionServiceImpl, times(0))
                .convertTickResponseToAuroraResponse(tick2);
        verify(subscriber, times(0)).onNext(response1);
        verify(subscriber, times(0)).onNext(response2);

    }

    @Test
    void request_ReturnSuperRequest() {

        auroraService.request(AURORA_REQUEST_BANICA, subscriber);

        verify(subscriber).onError(any(Status.UNIMPLEMENTED
                .withDescription("Method aurora.AuroraService/request is unimplemented")
                .asRuntimeException().getClass()));

    }

}