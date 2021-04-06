package com.market.banica.aurora.handlers;

import com.aurora.Aurora;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.market.banica.aurora.config.ChannelManager;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestHandlerTest {

    @InjectMocks
    private static RequestHandler requestHandler;

    private final ChannelManager channels = mock(ChannelManager.class);

    private static final Aurora.AuroraRequest AURORA_REQUEST_BANICA = Aurora.AuroraRequest.newBuilder().setTopic("market/banica").build();

    private static final StatusException STATUS_EXCEPTION =  Status.INVALID_ARGUMENT.asException();

    private final StreamObserver<Aurora.AuroraResponse> auroraResponse = mock(StreamObserver.class);

    private static final String TOPIC_PREFIX = "market";

    @Mock
    private final List<ManagedChannel> managedChannels = new ArrayList<>();

    private static final ManagedChannel MANAGED_CHANNEL_EUROPE = ManagedChannelBuilder
            .forAddress("localhost", 1030)
            .usePlaintext()
            .build();

    private static final ManagedChannel MANAGED_CHANNEL_ASIA = ManagedChannelBuilder
            .forAddress("localhost", 1040)
            .usePlaintext()
            .build();

    @Test
    void handleRequestWithAuroraRequestForNonExistentChannelInvokesOnError() {
        //Arrange
        when(channels.getAllChannelsContainingPrefix(TOPIC_PREFIX)).thenReturn(new ArrayList<>());

        //Act
        requestHandler.handleRequest(AURORA_REQUEST_BANICA, auroraResponse);

        //Assert
        verify(auroraResponse,times(1)).onError(any());
    }
}