package com.market.banica.aurora.mapper;

import com.aurora.Aurora;
import com.aurora.AuroraServiceGrpc;
import com.google.protobuf.Any;
import com.market.banica.aurora.config.ChannelManager;
import com.market.banica.aurora.config.StubManager;
import com.orderbook.CancelSubscriptionRequest;
import com.orderbook.CancelSubscriptionResponse;
import com.orderbook.InterestsRequest;
import com.orderbook.InterestsResponse;
import com.orderbook.ItemOrderBookRequest;
import com.orderbook.ItemOrderBookResponse;
import com.orderbook.OrderBookServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.management.ServiceNotFoundException;
import java.io.IOException;
import java.rmi.NoSuchObjectException;
import java.util.Optional;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestMapperTest {

    private final ManagedChannel dummyManagedChannel = ManagedChannelBuilder
            .forAddress("localhost", 1010)
            .usePlaintext()
            .build();

    private static final Aurora.AuroraRequest MARKET_REQUEST = Aurora.AuroraRequest.newBuilder().setTopic("market/eggs/10").build();
    private static final Aurora.AuroraRequest AURORA_REQUEST = Aurora.AuroraRequest.newBuilder().setTopic("aurora/eggs/10").build();
    private static final Aurora.AuroraRequest ORDERBOOK_REQUEST = Aurora.AuroraRequest.newBuilder().setTopic("orderbook/eggs/10").build();
    private static final Aurora.AuroraRequest ORDERBOOK_SUBSCRIBE_REQUEST = Aurora.AuroraRequest.newBuilder().setTopic("orderbook/eggs=subscribe").build();
    private static final Aurora.AuroraRequest ORDERBOOK_UNSUBSCRIBE_REQUEST = Aurora.AuroraRequest.newBuilder().setTopic("orderbook/eggs=unsubscribe").build();
    private static final Aurora.AuroraRequest INVALID_REQUEST = Aurora.AuroraRequest.newBuilder().setTopic("market/banica").build();

    private String auroraReceiverName;
    private ManagedChannel auroraReceiverChannel;

    private String orderBookReceiverName;
    private ManagedChannel orderBookReceiverChannel;

    private OrderBookServiceGrpc.OrderBookServiceBlockingStub orderBookBlockingStub;
    private AuroraServiceGrpc.AuroraServiceBlockingStub auroraBlockingStub;

    @Rule
    public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    @Mock
    private ChannelManager channelManager;

    @Mock
    private StubManager stubManager;

    @InjectMocks
    @Spy
    private RequestMapper requestMapper;

    @BeforeEach
    public void setUp() {
        auroraReceiverName = InProcessServerBuilder.generateName();
        auroraReceiverChannel = InProcessChannelBuilder
                .forName(auroraReceiverName)
                .executor(Executors.newSingleThreadExecutor()).build();

        auroraBlockingStub = AuroraServiceGrpc.newBlockingStub(auroraReceiverChannel);

        orderBookReceiverName = InProcessServerBuilder.generateName();
        orderBookReceiverChannel = InProcessChannelBuilder
                .forName(orderBookReceiverName)
                .executor(Executors.newSingleThreadExecutor()).build();

        orderBookBlockingStub = OrderBookServiceGrpc.newBlockingStub(orderBookReceiverChannel);
    }


    @Test
    void renderRequestWithRequestForNonExistentDestinationThrowsException() {
        when(channelManager.getChannelByKey(any())).thenReturn(Optional.empty());
        assertThrows(NoSuchObjectException.class, () -> requestMapper.renderRequest(INVALID_REQUEST));
    }

    @Test
    void renderRequestWithRequestForDestinationMarketThrowsException() {
        when(channelManager.getChannelByKey(any())).thenReturn(Optional.ofNullable(dummyManagedChannel));
        assertThrows(ServiceNotFoundException.class, () -> requestMapper.renderRequest(MARKET_REQUEST));
    }

    @Test
    void renderRequestWithRequestForOrderBookWithTopicSplitLengthOfThreeProcessesItemOrderBookRequest() throws IOException, ServiceNotFoundException {
        when(channelManager.getChannelByKey(any())).thenReturn(Optional.ofNullable(dummyManagedChannel));
        when(stubManager.getOrderbookBlockingStub(any())).thenReturn(orderBookBlockingStub);

        createFakeOrderBookReplyingServer();

        grpcCleanup.register(orderBookReceiverChannel);

        orderBookReceiverChannel.getState(true);

        ItemOrderBookResponse expectedOrderBookResponse = ItemOrderBookResponse.newBuilder().setItemName("eggs").build();

        Aurora.AuroraResponse actual = requestMapper.renderRequest(ORDERBOOK_REQUEST);

        assertEquals(expectedOrderBookResponse, actual.getMessage().unpack(ItemOrderBookResponse.class));
    }

    @Test
    void renderRequestWithSubscribeRequestForOrderBookProcessesSubscribeForItem() throws IOException, ServiceNotFoundException {
        when(channelManager.getChannelByKey(any())).thenReturn(Optional.ofNullable(dummyManagedChannel));
        when(stubManager.getOrderbookBlockingStub(any())).thenReturn(orderBookBlockingStub);

        createFakeOrderBookReplyingServer();

        grpcCleanup.register(orderBookReceiverChannel);

        orderBookReceiverChannel.getState(true);

        InterestsResponse expectedOrderBookResponse = InterestsResponse.newBuilder().build();

        Aurora.AuroraResponse actual = requestMapper.renderRequest(ORDERBOOK_SUBSCRIBE_REQUEST);

        assertEquals(expectedOrderBookResponse, actual.getMessage().unpack(InterestsResponse.class));
    }

    @Test
    void renderRequestWithUnsubscribeRequestForOrderBookProcessesCancelSubscription() throws IOException, ServiceNotFoundException {
        when(channelManager.getChannelByKey(any())).thenReturn(Optional.ofNullable(dummyManagedChannel));
        when(stubManager.getOrderbookBlockingStub(any())).thenReturn(orderBookBlockingStub);

        createFakeOrderBookReplyingServer();

        grpcCleanup.register(orderBookReceiverChannel);

        orderBookReceiverChannel.getState(true);

        CancelSubscriptionResponse expectedOrderBookResponse = CancelSubscriptionResponse.newBuilder().build();

        Aurora.AuroraResponse actual = requestMapper.renderRequest(ORDERBOOK_UNSUBSCRIBE_REQUEST);

        assertEquals(expectedOrderBookResponse, actual.getMessage().unpack(CancelSubscriptionResponse.class));
    }

    @Test
    void renderRequestWithDestinationAuroraSendsRequestAndReceivesResponseFromFakeAuroraService() throws IOException, ServiceNotFoundException {
        when(channelManager.getChannelByKey(any())).thenReturn(Optional.ofNullable(dummyManagedChannel));
        when(stubManager.getAuroraBlockingStub(any())).thenReturn(auroraBlockingStub);

        createFakeAuroraReplyingServer();

        grpcCleanup.register(auroraReceiverChannel);

        auroraReceiverChannel.getState(true);

        Aurora.AuroraResponse actual = requestMapper.renderRequest(AURORA_REQUEST);

        assertEquals(AURORA_REQUEST, actual.getMessage().unpack(Aurora.AuroraRequest.class));
    }


    private void createFakeOrderBookReplyingServer() throws IOException {
        grpcCleanup.register(InProcessServerBuilder
                .forName(orderBookReceiverName)
                .directExecutor()
                .addService(this.fakeReceivingOrderBookService())
                .build()
                .start());
    }

    private void createFakeAuroraReplyingServer() throws IOException {
        grpcCleanup.register(InProcessServerBuilder
                .forName(auroraReceiverName)
                .directExecutor()
                .addService(this.fakeReceivingAuroraService())
                .build()
                .start());
    }


    private OrderBookServiceGrpc.OrderBookServiceImplBase fakeReceivingOrderBookService() {
        return new OrderBookServiceGrpc.OrderBookServiceImplBase() {
            @Override
            public void getOrderBookItemLayers(ItemOrderBookRequest request, StreamObserver<ItemOrderBookResponse> responseObserver) {
                responseObserver.onNext(ItemOrderBookResponse.newBuilder()
                        .setItemName(request.getItemName())
                        .build());

                responseObserver.onCompleted();
            }

            @Override
            public void announceItemInterest(InterestsRequest request, StreamObserver<InterestsResponse> responseObserver) {
                responseObserver.onNext(InterestsResponse.newBuilder()
                        .build());

                responseObserver.onCompleted();
            }

            @Override
            public void cancelItemSubscription(CancelSubscriptionRequest request, StreamObserver<CancelSubscriptionResponse> responseObserver) {
                responseObserver.onNext(CancelSubscriptionResponse.newBuilder()
                        .build());

                responseObserver.onCompleted();
            }
        };
    }

    private AuroraServiceGrpc.AuroraServiceImplBase fakeReceivingAuroraService() {
        return new AuroraServiceGrpc.AuroraServiceImplBase() {
            @Override
            public void request(Aurora.AuroraRequest request, StreamObserver<Aurora.AuroraResponse> responseObserver) {
                Aurora.AuroraResponse response = Aurora.AuroraResponse
                        .newBuilder()
                        .setMessage(Any.pack(request))
                        .build();

                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        };
    }
}
