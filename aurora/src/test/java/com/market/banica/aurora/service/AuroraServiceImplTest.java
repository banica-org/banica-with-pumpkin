package com.market.banica.aurora.service;

import com.aurora.Aurora;
import com.market.banica.aurora.handlers.RequestHandler;
import com.market.banica.aurora.handlers.SubscribeHandler;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class AuroraServiceImplTest {

    private static final Aurora.AuroraRequest AURORA_REQUEST_BANICA = Aurora.AuroraRequest.newBuilder()
            .setTopic("market/banica").setClientId("orderBook").build();

    @Mock
    private RequestHandler requestHandler;

    @Mock
    private SubscribeHandler subscribeHandler;

    private final StreamObserver<Aurora.AuroraResponse> responseObserver = mock(StreamObserver.class);

    @InjectMocks
    private AuroraServiceImpl auroraService;


    @Test
    void requestVerifiesThatHandleRequestIsCalled() {
        auroraService.request(AURORA_REQUEST_BANICA, responseObserver);
        Mockito.verify(requestHandler, times(1)).handleRequest(AURORA_REQUEST_BANICA, responseObserver);
    }

    @Test
    void subscribeVerifiesThatHandleSubscribeIsCalled() {
        auroraService.subscribe(AURORA_REQUEST_BANICA, responseObserver);
        Mockito.verify(subscribeHandler, times(1)).handleSubscribe(AURORA_REQUEST_BANICA, responseObserver);
    }
}