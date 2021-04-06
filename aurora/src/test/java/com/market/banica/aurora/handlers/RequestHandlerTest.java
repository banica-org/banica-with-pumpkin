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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestHandlerTest {

    private static final ChannelManager channels = mock(ChannelManager.class);

    private static final Aurora.AuroraRequest AURORA_REQUEST_BANICA = Aurora.AuroraRequest.newBuilder().setTopic("market/banica").build();

    private static final String TOPIC_PREFIX = "market";

    private static final ManagedChannel MANAGED_CHANNEL_EUROPE = ManagedChannelBuilder
            .forAddress("localhost", 1010)
            .usePlaintext()
            .build();

    private final StreamObserver<Aurora.AuroraResponse> auroraResponse = mock(StreamObserver.class);

    @InjectMocks
    @Spy
    private static RequestHandler requestHandler;

    @Test
    void handleRequestWithAuroraRequestForNonExistentChannelInvokesOnError() {
        //Arrange
        when(channels.getChannelByKey(TOPIC_PREFIX)).thenReturn(Optional.empty());

        //Act
        requestHandler.handleRequest(AURORA_REQUEST_BANICA, auroraResponse);

        //Assert
        verify(auroraResponse, times(1)).onError(any());
    }

    @Test
    void handleRequestWithAuroraRequestForExistingChannelStartsToProcessTheRequest() {
        //Arrange
        when(channels.getChannelByKey(TOPIC_PREFIX)).thenReturn(Optional.ofNullable(MANAGED_CHANNEL_EUROPE));

        //Act
        requestHandler.handleRequest(AURORA_REQUEST_BANICA, auroraResponse);

        //Assert
        verify(requestHandler, times(1)).generateAuroraStub(any());
        verify(auroraResponse, times(1)).onError(any());
    }

    @Test
    void generateAuroraStubGeneratesStubCorrectly() {
        Channel expected = AuroraServiceGrpc.newStub(MANAGED_CHANNEL_EUROPE).getChannel();
        Channel actual = requestHandler.generateAuroraStub(MANAGED_CHANNEL_EUROPE).getChannel();
        assertEquals(expected, actual);
    }
}