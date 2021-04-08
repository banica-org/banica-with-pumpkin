package com.market.banica.generator.service;

import com.aurora.Aurora;
import com.google.protobuf.InvalidProtocolBufferException;
import com.market.Origin;
import com.market.TickResponse;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.ServerCallStreamObserver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketSubscriptionManagerTest {

    @InjectMocks
    private MarketSubscriptionManager marketSubscriptionManager;

    @SuppressWarnings("unchecked")
    private final ServerCallStreamObserver<Aurora.AuroraResponse> subscriberOne = mock(ServerCallStreamObserver.class);
    @SuppressWarnings("unchecked")
    private final ServerCallStreamObserver<Aurora.AuroraResponse> subscriberTwo = mock(ServerCallStreamObserver.class);

    private static final String GOOD_BANICA = "banica";
    private static final String GOOD_EGGS = "eggs";

    private static final Aurora.AuroraRequest AURORA_REQUEST_BANICA = Aurora.AuroraRequest.newBuilder()
            .setTopic("market/" + GOOD_BANICA)
            .build();
    private static final Aurora.AuroraRequest AURORA_REQUEST_EGGS = Aurora.AuroraRequest.newBuilder()
            .setTopic("market/" + GOOD_EGGS)
            .build();
    private static final Aurora.AuroraRequest AURORA_REQUEST_EMPTY = Aurora.AuroraRequest.newBuilder().build();
    private static final Aurora.AuroraRequest AURORA_REQUEST_INVALID = Aurora.AuroraRequest.newBuilder()
            .setTopic("market-eggs").build();


    @Test
    void subscribe_SubscribesOnlySubscribersWithSameGoodName() {

        TickResponse tickResponse = TickResponse.newBuilder()
                .setOrigin(Origin.AMERICA)
                .setGoodName(GOOD_BANICA)
                .setQuantity(10)
                .setPrice(2.4)
                .setTimestamp(new Date().getTime())
                .build();
        Aurora.AuroraResponse response = marketSubscriptionManager.convertTickResponseToAuroraResponse(tickResponse);


        marketSubscriptionManager.subscribe(AURORA_REQUEST_BANICA, subscriberOne);
        marketSubscriptionManager.subscribe(AURORA_REQUEST_EGGS, subscriberTwo);
        marketSubscriptionManager.notifySubscribers(tickResponse);


        verify(subscriberOne, times(1)).onNext(response);
        verify(subscriberTwo, times(0)).onNext(response);

    }

    @Test
    void subscribeForItemWithInputParameterWithInvalidGoodNameThrowsException() {

        assertThrows(IllegalArgumentException.class, () -> marketSubscriptionManager
                .subscribe(AURORA_REQUEST_INVALID, subscriberOne));

    }

    @Test
    void notifySubscribers_SpecificSubscriberToSpecificFood() {

        marketSubscriptionManager.subscribe(AURORA_REQUEST_BANICA, subscriberOne);
        marketSubscriptionManager.subscribe(AURORA_REQUEST_EGGS, subscriberTwo);

        TickResponse banicaTick = TickResponse.newBuilder()
                .setOrigin(Origin.AMERICA)
                .setGoodName(GOOD_BANICA)
                .setQuantity(10)
                .setPrice(2.4)
                .setTimestamp(new Date().getTime())
                .build();
        Aurora.AuroraResponse banicaResponse = marketSubscriptionManager
                .convertTickResponseToAuroraResponse(banicaTick);

        TickResponse eggsTick = TickResponse.newBuilder()
                .setOrigin(Origin.ASIA)
                .setGoodName(GOOD_EGGS)
                .setQuantity(5)
                .setPrice(0.2)
                .setTimestamp(new Date().getTime())
                .build();
        Aurora.AuroraResponse eggsResponse = marketSubscriptionManager.convertTickResponseToAuroraResponse(eggsTick);


        marketSubscriptionManager.notifySubscribers(banicaTick);
        marketSubscriptionManager.notifySubscribers(eggsTick);


        verify(subscriberOne, times(1)).onNext(banicaResponse);
        verify(subscriberTwo, times(0)).onNext(banicaResponse);
        verify(subscriberOne, times(0)).onNext(eggsResponse);
        verify(subscriberTwo, times(1)).onNext(eggsResponse);

    }

    @Test
    void notifySubscribers_StreamCancelled_ReturnOnErrorAndRemoveSubscriber() {

        marketSubscriptionManager.subscribe(AURORA_REQUEST_BANICA, subscriberOne);
        TickResponse banicaTick = TickResponse.newBuilder()
                .setOrigin(Origin.AMERICA)
                .setGoodName(GOOD_BANICA)
                .setQuantity(10)
                .setPrice(2.4)
                .setTimestamp(new Date().getTime())
                .build();
        Aurora.AuroraResponse banicaResponse = marketSubscriptionManager
                .convertTickResponseToAuroraResponse(banicaTick);

        when(subscriberOne.isCancelled()).thenReturn(true);


        marketSubscriptionManager.notifySubscribers(banicaTick);
        marketSubscriptionManager.notifySubscribers(banicaTick);


        verify(subscriberOne, times(1)).isCancelled();
        verify(subscriberOne, times(1)).onError(any(Status.CANCELLED
                .withDescription("subscriberOne has stopped requesting product " + banicaTick.getGoodName())
                .asException().getClass()));
        verify(subscriberOne, times(0)).onNext(banicaResponse);

    }

    @Test
    void notifySubscribers_StreamThrowsRuntimeException_RemoveSubscriber() {

        marketSubscriptionManager.subscribe(AURORA_REQUEST_BANICA, subscriberOne);

        TickResponse banicaTick = TickResponse.newBuilder()
                .setOrigin(Origin.AMERICA)
                .setGoodName(GOOD_BANICA)
                .setQuantity(10)
                .setPrice(2.4)
                .setTimestamp(new Date().getTime())
                .build();
        Aurora.AuroraResponse banicaResponse = marketSubscriptionManager
                .convertTickResponseToAuroraResponse(banicaTick);

        doThrow(StatusRuntimeException.class).when(subscriberOne).onNext(banicaResponse);


        marketSubscriptionManager.notifySubscribers(banicaTick);
        marketSubscriptionManager.notifySubscribers(banicaTick);


        verify(subscriberOne, times(1)).onNext(banicaResponse);

    }

    @Test
    void getGoodNameFromRequest_ValidRequest_ReturnsValidGoodName() {

        String goodName = marketSubscriptionManager.getGoodNameFromRequest(AURORA_REQUEST_BANICA);

        assertEquals(GOOD_BANICA, goodName);

    }

    @Test
    void getGoodNameFromRequest_EmptyRequest_ThrowsException() {

        assertThrows(IllegalArgumentException.class,
                () -> marketSubscriptionManager.getGoodNameFromRequest(AURORA_REQUEST_EMPTY));

    }

    @Test
    void getGoodNameFromRequest_InvalidRequest_ThrowsException() {

        assertThrows(IllegalArgumentException.class,
                () -> marketSubscriptionManager.getGoodNameFromRequest(AURORA_REQUEST_INVALID));

    }

    @Test
    void convertTickResponseToAuroraResponse() throws InvalidProtocolBufferException {

        TickResponse tickResponse = TickResponse.newBuilder()
                .setOrigin(Origin.AMERICA)
                .setGoodName(GOOD_BANICA)
                .setQuantity(10)
                .setPrice(2.4)
                .setTimestamp(new Date().getTime())
                .build();

        Aurora.AuroraResponse response = marketSubscriptionManager.convertTickResponseToAuroraResponse(tickResponse);

        Assertions.assertEquals(tickResponse, response.getMessage().unpack(TickResponse.class));

    }

}