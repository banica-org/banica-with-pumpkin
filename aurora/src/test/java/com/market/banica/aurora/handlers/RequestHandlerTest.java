package com.market.banica.aurora.handlers;

import com.aurora.Aurora;
import com.market.banica.aurora.mapper.RequestMapper;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.management.ServiceNotFoundException;
import java.rmi.NoSuchObjectException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RequestHandlerTest {
    private static final Aurora.AuroraRequest AURORA_REQUEST_BANICA = Aurora.AuroraRequest.newBuilder().setClientId("1").setTopic("market/banica").build();

    private final StreamObserver<Aurora.AuroraResponse> auroraResponse = mock(StreamObserver.class);

    @Mock
    private RequestMapper requestMapper;

    @InjectMocks
    @Spy
    private static RequestHandler requestHandler;

    @Test
    void handleRequestWithLegalResponseFromRenderRequestInvokesOnNextAndOnCompleted() throws NoSuchObjectException, ServiceNotFoundException {
        //Arrange
        Mockito.when(requestMapper.renderRequest(AURORA_REQUEST_BANICA)).thenReturn(Aurora.AuroraResponse.newBuilder().build());

        //Act
        requestHandler.handleRequest(AURORA_REQUEST_BANICA, auroraResponse);

        //Assert
        verify(auroraResponse, times(1)).onNext(any());
        verify(auroraResponse, times(1)).onCompleted();
    }
}