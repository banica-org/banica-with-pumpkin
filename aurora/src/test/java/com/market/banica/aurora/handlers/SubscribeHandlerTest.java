package com.market.banica.aurora.handlers;

import com.aurora.Aurora;
import com.market.banica.aurora.config.ChannelManager;
import com.market.banica.aurora.mapper.SubscribeMapper;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

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

    private final StreamObserver<Aurora.AuroraResponse> auroraResponse = mock(StreamObserver.class);

    @Mock
    SubscribeMapper subscribeMapper;

    @InjectMocks
    @Spy
    private static SubscribeHandler subscribeHandler;

    @Test
    void handleSubscribeVerifiesRenderSubscribeMethod() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        //Arrange
        when(channels.getAllChannelsContainingPrefix(TOPIC_PREFIX)).thenReturn(new ArrayList<>());

        //Act
        subscribeHandler.handleSubscribe(AURORA_REQUEST_BANICA, auroraResponse);

        //Assert
        verify(subscribeMapper, times(1)).renderSubscribe(any(), any());
    }
}