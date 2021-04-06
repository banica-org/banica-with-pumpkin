package src.handlers;

import com.aurora.Aurora;
import com.aurora.AuroraServiceGrpc;
import com.market.banica.common.channel.ChannelRPCConfig;
import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import src.config.ChannelManager;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscribeHandlerTest {

    @InjectMocks
    private static SubscribeHandler subscribeHandler;

    private static final ChannelManager channels = mock(ChannelManager.class);

    private static final Aurora.AuroraRequest AURORA_REQUEST_BANICA = Aurora.AuroraRequest.newBuilder().setTopic("market/banica").build();

    private static final StatusException STATUS_EXCEPTION =  Status.INVALID_ARGUMENT.asException();

    private final StreamObserver<Aurora.AuroraResponse> auroraResponse = mock(StreamObserver.class);

    private static final String TOPIC_PREFIX = "market";

    @Mock
    private final List<ManagedChannel> managedChannels = new ArrayList<>();

    private static final ManagedChannel MANAGED_CHANNEL_EUROPE = ManagedChannelBuilder
            .forAddress("localhost", 1010)
            .usePlaintext()
            .build();

    private static final ManagedChannel MANAGED_CHANNEL_ASIA = ManagedChannelBuilder
            .forAddress("localhost", 1020)
            .usePlaintext()
            .build();

    @Test
    void handleSubscribeWithAuroraRequestForNonExistentChannelInvokesOnError() {
        //Arrange
        when(channels.getAllChannelsContainingPrefix(TOPIC_PREFIX)).thenReturn(new ArrayList<>());

        //Act
        subscribeHandler.handleSubscribe(AURORA_REQUEST_BANICA, auroraResponse);

        //Assert
        verify(auroraResponse,times(1)).onError(any());
    }

    @Test
    void handleSubscribeWithAuroraRequestForExistingChannelInvokesStubSubscribeMethod(){
        //Arrange
        managedChannels.add(MANAGED_CHANNEL_EUROPE);
        managedChannels.add(MANAGED_CHANNEL_ASIA);
        when(channels.getAllChannelsContainingPrefix(TOPIC_PREFIX)).thenReturn(managedChannels);

        //Act

        subscribeHandler.handleSubscribe(AURORA_REQUEST_BANICA, auroraResponse);

        //Assert
        verify(managedChannels,times(1)).forEach(any());
    }

   // @Test
//    void generateAuroraStub() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
//
//        //Arrange
//        managedChannels.add(MANAGED_CHANNEL_EUROPE);
//        managedChannels.add(MANAGED_CHANNEL_ASIA);
//        when(channels.getAllChannelsContainingPrefix(TOPIC_PREFIX)).thenReturn(managedChannels);
//
//
//        System.out.println(MANAGED_CHANNEL_ASIA.getState(true));
//
//        //Act
//        subscribeHandler.handleSubscribe(AURORA_REQUEST_BANICA, auroraResponse);
//
//
//
//
//        //Assert
//        verify(managedChannels,times(1)).forEach(any());
//        System.out.println(MANAGED_CHANNEL_EUROPE.getState(true));
//        System.out.println(MANAGED_CHANNEL_ASIA.getState(true));
//        verify(auroraResponse,times(1)).onNext(any());
//       // assertEquals(false, op.equals(op2));
//
//    }
}