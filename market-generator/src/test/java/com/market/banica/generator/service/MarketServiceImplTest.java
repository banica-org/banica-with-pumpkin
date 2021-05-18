package com.market.banica.generator.service;

import com.market.AvailabilityResponse;
import com.market.BuySellProductResponse;
import com.market.CatalogueRequest;
import com.market.CatalogueResponse;
import com.market.MarketDataRequest;
import com.market.ProductBuySellRequest;
import com.market.TickResponse;
import com.market.banica.common.exception.ProductNotAvailableException;
import com.market.banica.generator.model.MarketTick;
import com.market.banica.generator.service.grpc.MarketService;
import io.grpc.Status;
import io.grpc.stub.ServerCallStreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketServiceImplTest {

    @Mock
    private MarketSubscriptionManager marketSubscriptionServiceImpl;

    @Mock
    private MarketState marketState;

    @InjectMocks
    private MarketService marketService;

    private final String GOOD_BANICA = "banica";
    private final long AMOUNT = 1;
    private final double PRICE_1 = 1.0;
    private final double PRICE_2 = 2.0;
    private final long TIMESTAMP = System.currentTimeMillis();


    private final MarketDataRequest MARKET_DATA_REQUEST = MarketDataRequest.newBuilder().setGoodName(GOOD_BANICA).build();
    private final ProductBuySellRequest MARKET_SELL_PRODUCT_REQUEST = ProductBuySellRequest.newBuilder().setItemName(GOOD_BANICA).setItemPrice(PRICE_1).setItemQuantity(AMOUNT).build();

    private final Map<String, Map<Double, MarketTick>> pendingOrders = new HashMap<>();

    @BeforeEach
    public void setUpPendingOrders() {
        ReflectionTestUtils.setField(marketService, "pendingOrders", pendingOrders);
    }

    @SuppressWarnings("unchecked")
    private final ServerCallStreamObserver<TickResponse> subscriberSubscribe = mock(ServerCallStreamObserver.class);
    @SuppressWarnings("unchecked")
    private final ServerCallStreamObserver<CatalogueResponse> subscriberRequest = mock(ServerCallStreamObserver.class);
    @SuppressWarnings("unchecked")
    private final ServerCallStreamObserver<AvailabilityResponse> calculatorAvailability = mock(ServerCallStreamObserver.class);
    @SuppressWarnings("unchecked")
    private final ServerCallStreamObserver<BuySellProductResponse> calculatorReturnProduct = mock(ServerCallStreamObserver.class);
    @SuppressWarnings("unchecked")
    private final ServerCallStreamObserver<BuySellProductResponse> sellProductStreamObserver = mock(ServerCallStreamObserver.class);

    @Test
    void subscribe_ValidRequest_BootstrapAndAddSubscriber() {
        TickResponse tick1 = TickResponse.newBuilder()
                .setGoodName(GOOD_BANICA)
                .setTimestamp(new Date().getTime() - 1000)
                .build();
        TickResponse tick2 = TickResponse.newBuilder()
                .setGoodName(GOOD_BANICA)
                .setTimestamp(new Date().getTime())
                .build();

        List<TickResponse> ticks = Arrays.asList(tick1, tick2);

        when(marketState.generateMarketTicks(GOOD_BANICA)).thenReturn(ticks);

        marketService.subscribeForItem(MARKET_DATA_REQUEST, subscriberSubscribe);

        verify(marketSubscriptionServiceImpl, times(1))
                .subscribe(MARKET_DATA_REQUEST, subscriberSubscribe);
        verify(marketState, times(1)).generateMarketTicks(GOOD_BANICA);
        verify(subscriberSubscribe, times(1)).onNext(tick1);
        verify(subscriberSubscribe, times(1)).onNext(tick2);

    }

    @Test
    void subscribe_StreamCancelled_BootstrapFailAndNoAddedSubscriber() {
        TickResponse tick1 = TickResponse.newBuilder()
                .setGoodName(GOOD_BANICA)
                .build();
        TickResponse tick2 = TickResponse.newBuilder()
                .setGoodName(GOOD_BANICA)
                .build();

        List<TickResponse> ticks = Arrays.asList(tick1, tick2);

        when(marketState.generateMarketTicks(GOOD_BANICA)).thenReturn(ticks);
        when(subscriberSubscribe.isCancelled()).thenReturn(true);

        marketService.subscribeForItem(MARKET_DATA_REQUEST, subscriberSubscribe);

        verify(marketSubscriptionServiceImpl, times(0)).subscribe(MARKET_DATA_REQUEST, subscriberSubscribe);
        verify(marketState, times(1)).generateMarketTicks(GOOD_BANICA);
        verify(subscriberSubscribe, times(0)).onNext(tick1);
        verify(subscriberSubscribe, times(0)).onNext(tick2);

    }

    @Test
    void request_ReturnSuperRequest() {
        CatalogueRequest request = CatalogueRequest.newBuilder().build();
        marketService.requestCatalogue(request, subscriberRequest);

        verify(subscriberRequest).onError(any(Status.UNIMPLEMENTED.withDescription("Method aurora.AuroraService/request is unimplemented").asRuntimeException().getClass()));

    }

    @Test
    public void checkAvailabilityAddsNewRecordInEmptyPendingOrders() throws ProductNotAvailableException {
        ProductBuySellRequest availabilityRequest = populateBuySellRequest(GOOD_BANICA, AMOUNT, PRICE_1);
        MarketTick marketTick = new MarketTick(GOOD_BANICA, AMOUNT, PRICE_1, TIMESTAMP);

        when(marketState.removeItemFromState(GOOD_BANICA, AMOUNT, PRICE_1)).thenReturn(marketTick);

        marketService.checkAvailability(availabilityRequest, calculatorAvailability);

        MarketTick marketTickInPendingOrders = pendingOrders.get(GOOD_BANICA).get(PRICE_1);

        assertEquals(1, pendingOrders.size());
        assertEquals(1, marketTickInPendingOrders.getQuantity());
        verify(marketState, times(1)).removeItemFromState(GOOD_BANICA, AMOUNT, PRICE_1);
    }

    @Test
    public void checkAvailabilityIncreasesPreviousRecordQuantityInPendingOrders() throws ProductNotAvailableException {
        ProductBuySellRequest availabilityRequest = populateBuySellRequest(GOOD_BANICA, AMOUNT, PRICE_1);
        MarketTick marketTick = new MarketTick(GOOD_BANICA, AMOUNT, PRICE_1, TIMESTAMP);

        when(marketState.removeItemFromState(GOOD_BANICA, AMOUNT, PRICE_1)).thenReturn(marketTick);

        marketService.checkAvailability(availabilityRequest, calculatorAvailability);
        marketService.checkAvailability(availabilityRequest, calculatorAvailability);

        MarketTick marketTickWithIncreasedQuantity = pendingOrders.get(GOOD_BANICA).get(PRICE_1);

        assertEquals(1, pendingOrders.size());
        assertEquals(2, marketTickWithIncreasedQuantity.getQuantity());
        verify(marketState, times(2)).removeItemFromState(GOOD_BANICA, AMOUNT, PRICE_1);
    }

    @Test
    public void checkAvailabilityAddsSecondRecordInPendingOrders() throws ProductNotAvailableException {
        ProductBuySellRequest availabilityRequest = populateBuySellRequest(GOOD_BANICA, AMOUNT, PRICE_1);
        ProductBuySellRequest secondAvailabilityRequest = populateBuySellRequest(GOOD_BANICA, AMOUNT, PRICE_2);
        MarketTick marketTick = new MarketTick(GOOD_BANICA, AMOUNT, PRICE_1, TIMESTAMP);
        MarketTick secondMarketTick = new MarketTick(GOOD_BANICA, AMOUNT, PRICE_2, TIMESTAMP);

        when(marketState.removeItemFromState(GOOD_BANICA, AMOUNT, PRICE_1)).thenReturn(marketTick);
        when(marketState.removeItemFromState(GOOD_BANICA, AMOUNT, PRICE_2)).thenReturn(secondMarketTick);

        marketService.checkAvailability(availabilityRequest, calculatorAvailability);
        marketService.checkAvailability(secondAvailabilityRequest, calculatorAvailability);

        assertEquals(2, pendingOrders.get(GOOD_BANICA).size());
        verify(marketState, times(1)).removeItemFromState(GOOD_BANICA, AMOUNT, PRICE_1);
        verify(marketState, times(1)).removeItemFromState(GOOD_BANICA, AMOUNT, PRICE_2);
    }

    @Test
    public void returnPendingProductClearsPendingOrders() {
        MarketTick marketTick = new MarketTick(GOOD_BANICA, 10, PRICE_1, TIMESTAMP);
        pendingOrders.put(GOOD_BANICA, new TreeMap<>());
        pendingOrders.get(GOOD_BANICA).put(PRICE_1, marketTick);

        ProductBuySellRequest request = populateBuySellRequest(GOOD_BANICA, 10, PRICE_1);

        marketService.returnPendingProduct(request, calculatorReturnProduct);

        assertEquals(0, pendingOrders.size());
    }

    @Test
    public void returnPendingProductRemovesMarketTickFromPendingOrders() {
        long amount = 10;
        MarketTick marketTick = new MarketTick(GOOD_BANICA, amount, PRICE_1, TIMESTAMP);
        MarketTick secondMarketTick = new MarketTick(GOOD_BANICA, amount, PRICE_2, TIMESTAMP);
        pendingOrders.put(GOOD_BANICA, new TreeMap<>());
        pendingOrders.get(GOOD_BANICA).put(PRICE_1, marketTick);
        pendingOrders.get(GOOD_BANICA).put(PRICE_2, secondMarketTick);

        ProductBuySellRequest request = populateBuySellRequest(GOOD_BANICA, amount, PRICE_1);

        marketService.returnPendingProduct(request, calculatorReturnProduct);

        assertEquals(1, pendingOrders.size());
        assertEquals(amount, pendingOrders.get(GOOD_BANICA).get(PRICE_2).getQuantity());
    }

    @Test
    public void returnPendingProductDecreasesMarketTickFromPendingOrders() {
        long expectedQuantity = 4;
        MarketTick marketTick = new MarketTick(GOOD_BANICA, 10, PRICE_1, TIMESTAMP);
        pendingOrders.put(GOOD_BANICA, new TreeMap<>());
        pendingOrders.get(GOOD_BANICA).put(PRICE_1, marketTick);

        ProductBuySellRequest request = populateBuySellRequest(GOOD_BANICA, 6, PRICE_1);

        marketService.returnPendingProduct(request, calculatorReturnProduct);

        assertEquals(1, pendingOrders.size());
        assertEquals(expectedQuantity, pendingOrders.get(GOOD_BANICA).get(PRICE_1).getQuantity());
    }

    @Test
    public void sellProductSellProductSuccessfullyToMarket() {
        this.marketService.sellProduct(MARKET_SELL_PRODUCT_REQUEST, sellProductStreamObserver);

        verify(sellProductStreamObserver, times(1)).onNext(any());
        verify(sellProductStreamObserver, times(1)).onCompleted();

    }

    private ProductBuySellRequest populateBuySellRequest(String goodName, long amount, double price) {
        return ProductBuySellRequest.newBuilder()
                .setItemName(goodName)
                .setItemPrice(price)
                .setItemQuantity(amount)
                .setMarketName("europe")
                .build();
    }
}