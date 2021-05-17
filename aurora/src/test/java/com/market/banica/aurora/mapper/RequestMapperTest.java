package com.market.banica.aurora.mapper;

import com.aurora.Aurora;
import com.aurora.AuroraServiceGrpc;
import com.market.AvailabilityResponse;
import com.market.BuySellProductResponse;
import com.market.MarketServiceGrpc;
import com.market.banica.aurora.config.ChannelManager;
import com.market.banica.aurora.config.StubManager;
import com.market.banica.aurora.util.FakeServerGenerator;
import com.orderbook.CancelSubscriptionResponse;
import com.orderbook.InterestsResponse;
import com.orderbook.ItemOrderBookResponse;
import com.orderbook.OrderBookServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.Rule;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestMapperTest {

    public static final Aurora.AuroraRequest MARKET_AVAILABILITY_REQUEST = Aurora.AuroraRequest
            .newBuilder()
            .setTopic("market-europe/availability/eggs/2.50/2")
            .build();
    public static final Aurora.AuroraRequest MARKET_RETURN_PENDING_PRODUCT_REQUEST = Aurora.AuroraRequest
            .newBuilder()
            .setTopic("market-europe/return/eggs/2.50/2")
            .build();
    public static final Aurora.AuroraRequest MARKET_BUY_PRODUCT_REQUEST = Aurora.AuroraRequest
            .newBuilder()
            .setTopic("market-europe/buy/eggs/2.50/2")
            .build();
    private static final Aurora.AuroraRequest AURORA_REQUEST = Aurora.AuroraRequest
            .newBuilder()
            .setTopic("aurora/eggs/10")
            .build();
    private static final Aurora.AuroraRequest ORDERBOOK_REQUEST = Aurora.AuroraRequest
            .newBuilder()
            .setTopic("orderbook/eggs/10")
            .build();
    private static final Aurora.AuroraRequest ORDERBOOK_SUBSCRIBE_REQUEST = Aurora.AuroraRequest
            .newBuilder()
            .setTopic("orderbook/eggs=subscribe")
            .build();
    private static final Aurora.AuroraRequest ORDERBOOK_UNSUBSCRIBE_REQUEST = Aurora.AuroraRequest
            .newBuilder()
            .setTopic("orderbook/eggs=unsubscribe")
            .build();
    private static final Aurora.AuroraRequest INVALID_REQUEST = Aurora.AuroraRequest
            .newBuilder()
            .setTopic("market/banica")
            .build();

    private static final ManagedChannel DUMMY_MANAGED_CHANNEL = ManagedChannelBuilder
            .forAddress("localhost", 1010)
            .usePlaintext()
            .build();

    private static final String AURORA_SERVER_NAME = "auroraServer";
    private static final String ORDER_BOOK_SERVER_NAME = "orderBookServer";
    private static final String MARKET_SERVER_NAME = "marketServer";

    private static final ManagedChannel AURORA_SERVER_CHANNEL = InProcessChannelBuilder
            .forName(AURORA_SERVER_NAME)
            .executor(Executors.newSingleThreadExecutor()).build();

    private static final ManagedChannel ORDER_BOOK_SERVER_CHANNEL = InProcessChannelBuilder
            .forName(ORDER_BOOK_SERVER_NAME)
            .executor(Executors.newSingleThreadExecutor()).build();

    private static final ManagedChannel MARKET_SERVER_CHANNEL = InProcessChannelBuilder
            .forName(MARKET_SERVER_NAME)
            .executor(Executors.newSingleThreadExecutor()).build();
    @Rule
    public static GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();
    private final OrderBookServiceGrpc.OrderBookServiceBlockingStub orderBookBlockingStub = OrderBookServiceGrpc
            .newBlockingStub(ORDER_BOOK_SERVER_CHANNEL);
    private final AuroraServiceGrpc.AuroraServiceBlockingStub auroraBlockingStub = AuroraServiceGrpc
            .newBlockingStub(AURORA_SERVER_CHANNEL);
    private final MarketServiceGrpc.MarketServiceBlockingStub marketBlockingStub = MarketServiceGrpc
            .newBlockingStub(MARKET_SERVER_CHANNEL);
    @Mock
    private ChannelManager channelManager;

    @Mock
    private StubManager stubManager;

    @InjectMocks
    @Spy
    private RequestMapper requestMapper;

    @BeforeAll
    public static void setUp() throws IOException {
        FakeServerGenerator.createFakeServer(AURORA_SERVER_NAME, grpcCleanup, AURORA_SERVER_CHANNEL);
        FakeServerGenerator.createFakeServer(ORDER_BOOK_SERVER_NAME, grpcCleanup, ORDER_BOOK_SERVER_CHANNEL);
        FakeServerGenerator.createFakeServer(MARKET_SERVER_NAME, grpcCleanup, MARKET_SERVER_CHANNEL);

        FakeServerGenerator.addChannel("auroraServerChannel", AURORA_SERVER_CHANNEL);
        FakeServerGenerator.addChannel("orderBookServerChannel", ORDER_BOOK_SERVER_CHANNEL);
        FakeServerGenerator.addChannel("marketServerChannel", MARKET_SERVER_CHANNEL);
        FakeServerGenerator.addChannel("dummyChannel", DUMMY_MANAGED_CHANNEL);
    }

    @AfterAll
    public static void shutDownChannels() {
        FakeServerGenerator.shutDownAllChannels();
    }

    @Test
    void renderRequestWithRequestForNonExistentDestinationThrowsException() {
        when(channelManager.getChannelByKey(any())).thenReturn(Optional.empty());
        assertThrows(NoSuchObjectException.class, () -> requestMapper.renderRequest(INVALID_REQUEST));
    }

    @Test
    void renderRequestWithRequestForOrderBookWithTopicSplitLengthOfThreeProcessesItemOrderBookRequest() throws IOException, ServiceNotFoundException {
        //Arrange
        when(channelManager.getChannelByKey(any())).thenReturn(Optional.ofNullable(DUMMY_MANAGED_CHANNEL));
        when(stubManager.getOrderbookBlockingStub(any())).thenReturn(orderBookBlockingStub);
        ItemOrderBookResponse expectedOrderBookResponse = ItemOrderBookResponse
                .newBuilder()
                .setItemName("eggs")
                .build();
        //Act
        Aurora.AuroraResponse actual = requestMapper.renderRequest(ORDERBOOK_REQUEST);
        //Assert
        assertEquals(expectedOrderBookResponse, actual.getMessage().unpack(ItemOrderBookResponse.class));
    }

    @Test
    void renderRequestWithSubscribeRequestForOrderBookProcessesSubscribeForItem() throws IOException, ServiceNotFoundException {
        //Arrange
        when(channelManager.getChannelByKey(any())).thenReturn(Optional.ofNullable(DUMMY_MANAGED_CHANNEL));
        when(stubManager.getOrderbookBlockingStub(any())).thenReturn(orderBookBlockingStub);
        InterestsResponse expectedOrderBookResponse = InterestsResponse.newBuilder().build();
        //Act
        Aurora.AuroraResponse actual = requestMapper.renderRequest(ORDERBOOK_SUBSCRIBE_REQUEST);
        //Assert
        assertEquals(expectedOrderBookResponse, actual.getMessage().unpack(InterestsResponse.class));
    }

    @Test
    void renderRequestWithUnsubscribeRequestForOrderBookProcessesCancelSubscription() throws IOException, ServiceNotFoundException {
        //Arrange
        when(channelManager.getChannelByKey(any())).thenReturn(Optional.ofNullable(DUMMY_MANAGED_CHANNEL));
        when(stubManager.getOrderbookBlockingStub(any())).thenReturn(orderBookBlockingStub);
        CancelSubscriptionResponse expectedOrderBookResponse = CancelSubscriptionResponse.newBuilder().build();
        //Act
        Aurora.AuroraResponse actual = requestMapper.renderRequest(ORDERBOOK_UNSUBSCRIBE_REQUEST);
        //Assert
        assertEquals(expectedOrderBookResponse, actual.getMessage().unpack(CancelSubscriptionResponse.class));
    }

    @Test
    void renderRequestWithDestinationAuroraSendsRequestAndReceivesResponseFromFakeAuroraService() throws IOException, ServiceNotFoundException {
        //Arrange
        when(channelManager.getChannelByKey(any())).thenReturn(Optional.ofNullable(DUMMY_MANAGED_CHANNEL));
        when(stubManager.getAuroraBlockingStub(any())).thenReturn(auroraBlockingStub);
        //Act
        Aurora.AuroraResponse actual = requestMapper.renderRequest(AURORA_REQUEST);
        //Assert
        assertEquals(AURORA_REQUEST, actual.getMessage().unpack(Aurora.AuroraRequest.class));
    }

    @Test
    void renderRequestWithAvailabilityRequestForMarketToChekIfProductExist() throws IOException, ServiceNotFoundException {
        //Arrange
        when(channelManager.getChannelByKey(any())).thenReturn(Optional.ofNullable(DUMMY_MANAGED_CHANNEL));
        when(stubManager.getMarketBlockingStub(any())).thenReturn(marketBlockingStub);
        AvailabilityResponse availabilityResponse = AvailabilityResponse.newBuilder().build();
        //Act
        Aurora.AuroraResponse actual = requestMapper.renderRequest(MARKET_AVAILABILITY_REQUEST);
        //Assert
        assertEquals(availabilityResponse, actual.getMessage().unpack(AvailabilityResponse.class));
    }

    @Test
    void renderRequestWithBuyProductRequestForMarketToBuyTheProductFromMarket() throws IOException, ServiceNotFoundException {
        //Arrange
        when(channelManager.getChannelByKey(any())).thenReturn(Optional.ofNullable(DUMMY_MANAGED_CHANNEL));
        when(stubManager.getMarketBlockingStub(any())).thenReturn(marketBlockingStub);
        BuySellProductResponse buySellProductResponse = BuySellProductResponse.newBuilder().build();
        //Act
        Aurora.AuroraResponse actual = requestMapper.renderRequest(MARKET_BUY_PRODUCT_REQUEST);
        //Assert
        assertEquals(buySellProductResponse, actual.getMessage().unpack(BuySellProductResponse.class));
    }

    @Test
    void renderRequestWithReturnProductRequestForMarketToReturnTheProductFromMarket() throws IOException, ServiceNotFoundException {
        //Arrange
        when(channelManager.getChannelByKey(any())).thenReturn(Optional.ofNullable(DUMMY_MANAGED_CHANNEL));
        when(stubManager.getMarketBlockingStub(any())).thenReturn(marketBlockingStub);
        BuySellProductResponse buySellProductResponse = BuySellProductResponse.newBuilder().build();
        //Act
        Aurora.AuroraResponse actual = requestMapper.renderRequest(MARKET_RETURN_PENDING_PRODUCT_REQUEST);
        //Assert
        assertEquals(buySellProductResponse, actual.getMessage().unpack(BuySellProductResponse.class));
    }
}
