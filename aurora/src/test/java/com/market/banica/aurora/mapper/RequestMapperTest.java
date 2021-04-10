package com.market.banica.aurora.mapper;

import com.aurora.Aurora;
import com.market.banica.aurora.config.ChannelManager;
import com.market.banica.aurora.config.StubManager;
import com.market.banica.aurora.service.AuroraServiceImpl;
import com.market.banica.common.channel.ChannelRPCConfig;
import com.orderbook.CancelSubscriptionRequest;
import com.orderbook.InterestsRequest;
import com.orderbook.ItemOrderBookRequest;
import com.orderbook.ItemOrderBookResponse;
import com.orderbook.OrderBookLayer;
import com.orderbook.OrderBookServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.management.ServiceNotFoundException;
import java.rmi.NoSuchObjectException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestMapperTest {

    private static final String HOST = "localhost";
    private static final int PORT = 1010;

    private ManagedChannel managedChannel = ManagedChannelBuilder
            .forAddress(HOST, PORT)
            .usePlaintext()
            .build();

    public static final String ORDERBOOK_TOPIC = "orderbook/eggs/10";
    public static final String AURORA_TOPIC = "aurora/eggs/10";
    public static final String MARKET_TOPIC = "market/eggs/10";


    private static final Aurora.AuroraRequest INVALID_REQUEST = Aurora.AuroraRequest.newBuilder().setTopic("market/banica").build();
    private static final Aurora.AuroraRequest ORDERBOOK_REQUEST = Aurora.AuroraRequest.newBuilder().setTopic(ORDERBOOK_TOPIC).build();
    private static final Aurora.AuroraRequest AURORA_REQUEST = Aurora.AuroraRequest.newBuilder().setTopic(AURORA_TOPIC).build();
    private static final Aurora.AuroraRequest MARKET_REQUEST = Aurora.AuroraRequest.newBuilder().setTopic(MARKET_TOPIC).build();
    private final StreamObserver<Aurora.AuroraResponse> auroraResponse = mock(StreamObserver.class);

    public static final String MARKET = "market";

    private static final String EGGS_ITEM_NAME = "eggs";

    private static final String CLIENT = "europe";

    public static final String SUBSCRIBE = "subscribe";

    private static final ItemOrderBookRequest ITEM_ORDER_BOOK_REQUEST =
            ItemOrderBookRequest.newBuilder().setClientId("calculator").setItemName("eggs").setQuantity(3).build();

    private static final InterestsRequest AURORA_ANNOUNCE_REQUEST =
            InterestsRequest.newBuilder().setItemName(MARKET + "/" + EGGS_ITEM_NAME).setClientId(CLIENT).build();

    private static final CancelSubscriptionRequest CANCEL_SUBSCRIPTION_REQUEST =
            CancelSubscriptionRequest.newBuilder().setItemName(MARKET + "/" + EGGS_ITEM_NAME).setClientId(CLIENT).build();

    List<OrderBookLayer> orderBookLayers = new ArrayList<>();
    private OrderBookServiceGrpc.OrderBookServiceBlockingStub blockingStub;

    @Rule
    public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    @Mock
    private  ChannelManager channelManager;

    @Mock
    private  StubManager stubManager;


    @Mock
    private AuroraServiceImpl auroraService;

    @InjectMocks
    @Spy
    private RequestMapper requestMapper;

    @SneakyThrows
    @Before
    public void setUp() {
        String serverName = InProcessServerBuilder.generateName();

        grpcCleanup.register(InProcessServerBuilder
                .forName(serverName).directExecutor().addService(auroraService).build().start());

        blockingStub = OrderBookServiceGrpc.newBlockingStub(
                grpcCleanup.register(InProcessChannelBuilder.forName(serverName).directExecutor().build()));
    }


    @Test
    void renderRequestWithRequestForNonExistentDestinationThrowsException() {
        when(channelManager.getChannelByKey(any())).thenReturn(Optional.empty());
        assertThrows(NoSuchObjectException.class, () -> requestMapper.renderRequest(INVALID_REQUEST));
    }

    @Test
    void renderRequestWithRequestForOrderBookRendersMapping() throws NoSuchObjectException, ServiceNotFoundException {
        when(channelManager.getChannelByKey(any())).thenReturn(Optional.ofNullable(managedChannel));
        OrderBookServiceGrpc.OrderBookServiceBlockingStub orderbookStub = OrderBookServiceGrpc.newBlockingStub(managedChannel);
        when(stubManager.getOrderbookBlockingStub(any())).thenReturn(orderbookStub);

        ItemOrderBookResponse orderBookResponse = ItemOrderBookResponse.newBuilder().setItemName("asd").build();


        when(orderbookStub.getOrderBookItemLayers(any())).thenReturn(orderBookResponse);

        Aurora.AuroraResponse actual =  requestMapper.renderRequest(ORDERBOOK_REQUEST);

        ItemOrderBookResponse bookItemLayers = blockingStub.getOrderBookItemLayers(ITEM_ORDER_BOOK_REQUEST);
       // assertEquals(3, bookItemLayers.getOrderbookLayersList().size());
    }
















}
