package com.market.banica.aurora.handlers;

import com.aurora.Aurora;
import com.aurora.AuroraServiceGrpc;
import com.market.banica.aurora.config.ChannelManager;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscribeHandlerTest {

    private static final ChannelManager channels = mock(ChannelManager.class);

    private static final Aurora.AuroraRequest AURORA_REQUEST_BANICA = Aurora.AuroraRequest.newBuilder().setTopic("market/banica").build();

    private static final String TOPIC_PREFIX = "market";

    private static final ManagedChannel MANAGED_CHANNEL_EUROPE = ManagedChannelBuilder
            .forAddress("localhost", 1010)
            .usePlaintext()
            .build();

    private static final ManagedChannel MANAGED_CHANNEL_ASIA = ManagedChannelBuilder
            .forAddress("localhost", 1020)
            .usePlaintext()
            .build();

    private final List<ManagedChannel> managedChannels = new ArrayList<>();

    private final StreamObserver<Aurora.AuroraResponse> auroraResponse = mock(StreamObserver.class);

    @InjectMocks
    @Spy
    private static SubscribeHandler subscribeHandler;

    @Test
    void handleSubscribeWithAuroraRequestForNonExistentChannelInvokesOnError() {
        //Arrange
        when(channels.getAllChannelsContainingPrefix(TOPIC_PREFIX)).thenReturn(new ArrayList<>());

        //Act
        subscribeHandler.handleSubscribe(AURORA_REQUEST_BANICA, auroraResponse);

        //Assert
        verify(auroraResponse, times(1)).onError(any());
    }

//    @Test
//    void handleSubscribeWithAuroraRequestForExistingChannelInvokesStubSubscribeMethod() {
//        //Arrange
//        managedChannels.add(MANAGED_CHANNEL_EUROPE);
//        managedChannels.add(MANAGED_CHANNEL_ASIA);
//        when(channels.getAllChannelsContainingPrefix(TOPIC_PREFIX)).thenReturn(managedChannels);
//
//        //Act
//        subscribeHandler.handleSubscribe(AURORA_REQUEST_BANICA, auroraResponse);
//
//        //Assert
//        verify(subscribeHandler, times(2)).generateAuroraStub(any());
//    }
//
//    @Test
//    void generateAuroraStubGeneratesStubCorrectly() {
//        Channel expected = AuroraServiceGrpc.newStub(MANAGED_CHANNEL_EUROPE).getChannel();
//        Channel actual = subscribeHandler.generateAuroraStub(MANAGED_CHANNEL_EUROPE).getChannel();
//        assertEquals(expected, actual);
//    }
}