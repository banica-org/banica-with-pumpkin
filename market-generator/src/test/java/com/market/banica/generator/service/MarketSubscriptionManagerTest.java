package com.market.banica.generator.service;

import com.market.MarketDataRequest;
import com.market.Origin;
import com.market.TickResponse;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.ServerCallStreamObserver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketSubscriptionManagerTest {

//    @InjectMocks
//    private MarketSubscriptionManager marketSubscriptionManager;
//
//    @SuppressWarnings("unchecked")
//    private final ServerCallStreamObserver<TickResponse> subscriberOne = mock(ServerCallStreamObserver.class);
//    @SuppressWarnings("unchecked")
//    private final ServerCallStreamObserver<TickResponse> subscriberTwo = mock(ServerCallStreamObserver.class);
//
//    private static final String GOOD_BANICA = "banica";
//    private static final String GOOD_EGGS = "eggs";
//
//    private static final MarketDataRequest MARKET_DATA_REQUEST_BANICA = MarketDataRequest.newBuilder()
//            .setGoodName(GOOD_BANICA)
//            .build();
//    private static final MarketDataRequest MARKET_DATA_REQUEST_EGGS = MarketDataRequest.newBuilder()
//            .setGoodName(GOOD_EGGS)
//            .build();
//
//    @Test
//    void subscribe_SubscribesOnlySubscribersWithSameGoodName() {
//
//        TickResponse tickResponse = TickResponse.newBuilder()
//                .setOrigin(Origin.AMERICA)
//                .setGoodName(GOOD_BANICA)
//                .setQuantity(10)
//                .setPrice(2.4)
//                .setTimestamp(new Date().getTime())
//                .build();
//
//        marketSubscriptionManager.subscribe(MARKET_DATA_REQUEST_BANICA, subscriberOne);
//        marketSubscriptionManager.subscribe(MARKET_DATA_REQUEST_EGGS, subscriberTwo);
//        marketSubscriptionManager.notifySubscribers(tickResponse);
//
//        verify(subscriberOne, times(1)).onNext(tickResponse);
//        verify(subscriberTwo, times(0)).onNext(tickResponse);
//
//    }
//
//    @Test
//    void notifySubscribers_SpecificSubscriberToSpecificFood() {
//
//        marketSubscriptionManager.subscribe(MARKET_DATA_REQUEST_BANICA, subscriberOne);
//        marketSubscriptionManager.subscribe(MARKET_DATA_REQUEST_EGGS, subscriberTwo);
//
//        TickResponse banicaTick = TickResponse.newBuilder()
//                .setOrigin(Origin.AMERICA)
//                .setGoodName(GOOD_BANICA)
//                .setQuantity(10)
//                .setPrice(2.4)
//                .setTimestamp(new Date().getTime())
//                .build();
//
//        TickResponse eggsTick = TickResponse.newBuilder()
//                .setOrigin(Origin.ASIA)
//                .setGoodName(GOOD_EGGS)
//                .setQuantity(5)
//                .setPrice(0.2)
//                .setTimestamp(new Date().getTime())
//                .build();
//
//
//        marketSubscriptionManager.notifySubscribers(banicaTick);
//        marketSubscriptionManager.notifySubscribers(eggsTick);
//
//
//        verify(subscriberOne, times(1)).onNext(banicaTick);
//        verify(subscriberTwo, times(0)).onNext(banicaTick);
//        verify(subscriberOne, times(0)).onNext(eggsTick);
//        verify(subscriberTwo, times(1)).onNext(eggsTick);
//
//    }
//
//    @Test
//    void notifySubscribers_StreamCancelled_ReturnOnErrorAndRemoveSubscriber() {
//
//        marketSubscriptionManager.subscribe(MARKET_DATA_REQUEST_BANICA, subscriberOne);
//        TickResponse banicaTick = TickResponse.newBuilder()
//                .setOrigin(Origin.AMERICA)
//                .setGoodName(GOOD_BANICA)
//                .setQuantity(10)
//                .setPrice(2.4)
//                .setTimestamp(new Date().getTime())
//                .build();
//
//        when(subscriberOne.isCancelled()).thenReturn(true);
//
//
//        marketSubscriptionManager.notifySubscribers(banicaTick);
//        marketSubscriptionManager.notifySubscribers(banicaTick);
//
//
//        verify(subscriberOne, times(1)).isCancelled();
//        verify(subscriberOne, times(1)).onError(any(Status.CANCELLED
//                .withDescription("subscriberOne has stopped requesting product " + banicaTick.getGoodName())
//                .asException().getClass()));
//        verify(subscriberOne, times(0)).onNext(banicaTick);
//
//    }
//
//    @Test
//    void notifySubscribers_StreamThrowsRuntimeException_RemoveSubscriber() {
//
//        marketSubscriptionManager.subscribe(MARKET_DATA_REQUEST_BANICA, subscriberOne);
//
//        TickResponse banicaTick = TickResponse.newBuilder()
//                .setOrigin(Origin.AMERICA)
//                .setGoodName(GOOD_BANICA)
//                .setQuantity(10)
//                .setPrice(2.4)
//                .setTimestamp(new Date().getTime())
//                .build();
//
//        doThrow(StatusRuntimeException.class).when(subscriberOne).onNext(banicaTick);
//
//
//        marketSubscriptionManager.notifySubscribers(banicaTick);
//        marketSubscriptionManager.notifySubscribers(banicaTick);
//
//
//        verify(subscriberOne, times(1)).onNext(banicaTick);
//
//    }

}