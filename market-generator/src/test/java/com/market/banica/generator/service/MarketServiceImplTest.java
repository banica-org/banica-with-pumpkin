package com.market.banica.generator.service;

import com.market.CatalogueRequest;
import com.market.CatalogueResponse;
import com.market.MarketDataRequest;
import com.market.TickResponse;
import com.market.banica.generator.service.grpc.MarketService;
import io.grpc.Status;
import io.grpc.stub.ServerCallStreamObserver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketServiceImplTest {

    @Mock
    private MarketSubscriptionManager marketSubscriptionServiceImpl;

    @Mock
    private MarketState marketState;

    @InjectMocks
    private MarketService marketService;

    private final String GOOD_BANICA = "banica";

    private final MarketDataRequest MARKET_DATA_REQUEST = MarketDataRequest.newBuilder()
            .setGoodName(GOOD_BANICA)
            .build();

    @SuppressWarnings("unchecked")
    private final ServerCallStreamObserver<TickResponse> subscriberSubscribe = mock(ServerCallStreamObserver.class);
    @SuppressWarnings("unchecked")
    private final ServerCallStreamObserver<CatalogueResponse> subscriberRequest = mock(ServerCallStreamObserver.class);

    @Test
    void subscribe_ValidRequest_BootstrapAndAddSubscriber() {

        TickResponse tick1 = TickResponse.newBuilder()
                .setGoodName(GOOD_BANICA)
                .setTimestamp(new Date().getTime() - 1000)
                .build();
        TickResponse tick2 = TickResponse.newBuilder()
                .setGoodName(GOOD_BANICA)
                .setTimestamp(new Date().getTime())
                .build();

        List<TickResponse> ticks = Arrays.asList(tick1, tick2);

        when(marketState.generateMarketTicks(GOOD_BANICA)).thenReturn(ticks);


        marketService.subscribeForItem(MARKET_DATA_REQUEST, subscriberSubscribe);


        verify(marketSubscriptionServiceImpl, times(1))
                .subscribe(MARKET_DATA_REQUEST, subscriberSubscribe);
        verify(marketState, times(1)).generateMarketTicks(GOOD_BANICA);
        verify(subscriberSubscribe, times(1)).onNext(tick1);
        verify(subscriberSubscribe, times(1)).onNext(tick2);

    }

    @Test
    void subscribe_StreamCancelled_BootstrapFailAndNoAddedSubscriber() {

        TickResponse tick1 = TickResponse.newBuilder()
                .setGoodName(GOOD_BANICA)
                .build();
        TickResponse tick2 = TickResponse.newBuilder()
                .setGoodName(GOOD_BANICA)
                .build();

        List<TickResponse> ticks = Arrays.asList(tick1, tick2);

        when(marketState.generateMarketTicks(GOOD_BANICA)).thenReturn(ticks);
        when(subscriberSubscribe.isCancelled()).thenReturn(true);


        marketService.subscribeForItem(MARKET_DATA_REQUEST, subscriberSubscribe);


        verify(marketSubscriptionServiceImpl, times(0))
                .subscribe(MARKET_DATA_REQUEST, subscriberSubscribe);
        verify(marketState, times(1)).generateMarketTicks(GOOD_BANICA);
        verify(subscriberSubscribe, times(0)).onNext(tick1);
        verify(subscriberSubscribe, times(0)).onNext(tick2);

    }

    @Test
    void request_ReturnSuperRequest() {

        CatalogueRequest request = CatalogueRequest.newBuilder().build();

        marketService.requestCatalogue(request, subscriberRequest);

        verify(subscriberRequest).onError(any(Status.UNIMPLEMENTED
                .withDescription("Method aurora.AuroraService/request is unimplemented")
                .asRuntimeException().getClass()));

    }

}