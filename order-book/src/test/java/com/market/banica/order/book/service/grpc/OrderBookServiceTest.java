package com.market.banica.order.book.service.grpc;

import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.InvalidProtocolBufferException;
import com.market.Origin;
import com.market.banica.common.exception.TrackingException;
import com.market.banica.common.util.ApplicationDirectoryUtil;
import com.market.banica.order.book.model.Item;
import com.market.banica.order.book.model.ItemMarket;
import com.market.banica.order.book.util.InterestsPersistence;
import com.orderbook.CancelSubscriptionRequest;
import com.orderbook.CancelSubscriptionResponse;
import com.orderbook.InterestsRequest;
import com.orderbook.InterestsResponse;
import com.orderbook.ItemOrderBookRequest;
import com.orderbook.ItemOrderBookResponse;
import com.orderbook.OrderBookLayer;
import com.orderbook.OrderBookServiceGrpc;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import lombok.SneakyThrows;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OrderBookServiceTest {

    private static final String EGGS_ITEM_NAME = "eggs";

    private static final String EXCEPTION_MESSAGE = "Mock method to throw exception.";

    private static final String CLIENT = "europe";

    private static final String MARKET = "market";

    private static final ItemOrderBookRequest ITEM_ORDER_BOOK_REQUEST =
            ItemOrderBookRequest.newBuilder().setClientId("calculator").setItemName("eggs").setQuantity(3).build();

    private static final InterestsRequest AURORA_ANNOUNCE_REQUEST =
            InterestsRequest.newBuilder().setItemName(EGGS_ITEM_NAME).setClientId(CLIENT).build();

    private static final CancelSubscriptionRequest CANCEL_SUBSCRIPTION_REQUEST =
            CancelSubscriptionRequest.newBuilder().setItemName(EGGS_ITEM_NAME).setClientId(CLIENT).build();

    List<OrderBookLayer> orderBookLayers = new ArrayList<>();
    private OrderBookServiceGrpc.OrderBookServiceBlockingStub blockingStub;

    @Rule
    public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    @Mock
    private ItemMarket itemMarket;

    @Mock
    private AuroraClient auroraClient;

    private String interestsFileName = "test-orderbookInterests.dat";

    private final InterestsPersistence interestsPersistence = mock(InterestsPersistence.class);
    private final Map<String, Set<String>> interestsMap = mock(Map.class);

    private OrderBookService orderBookService;

    @SneakyThrows
    @Before
    public void setUp() {
        orderBookService = new OrderBookService(auroraClient, itemMarket, interestsFileName);

        Set<Item> items = this.populateItems();
        populateList(items);

        String serverName = InProcessServerBuilder.generateName();

        grpcCleanup.register(InProcessServerBuilder
                .forName(serverName).directExecutor().addService(orderBookService).build().start());

        blockingStub = OrderBookServiceGrpc.newBlockingStub(
                grpcCleanup.register(InProcessChannelBuilder.forName(serverName).directExecutor().build()));

        ReflectionTestUtils.setField(orderBookService, "subscriptionExecutor", MoreExecutors.newDirectExecutorService());
        ReflectionTestUtils.setField(orderBookService, "interestsPersistence", interestsPersistence);
        ReflectionTestUtils.setField(orderBookService, "interestsMap", interestsMap);
        reset(interestsPersistence);
        reset(interestsMap);
    }

    @After
    public void teardown() throws IOException {

        File interestsFile = ApplicationDirectoryUtil.getConfigFile(interestsFileName);
        assert interestsFile.delete();

    }

    @Test
    public void getOrderBookItemLayersSuccessfullyReturnsItemOrderBookResponseWhenItemMarketContainsRequestItemName() throws InvalidProtocolBufferException {
        //Arrange
        when(itemMarket.getRequestedItem(any(), any(Long.class))).thenReturn(orderBookLayers);

        //Act
        ItemOrderBookResponse bookItemLayers = blockingStub.getOrderBookItemLayers(ITEM_ORDER_BOOK_REQUEST);

        //Assert
        assertEquals(3, bookItemLayers.getOrderbookLayersList().size());
        assertEquals(1.2, bookItemLayers.getOrderbookLayersList().get(0).getPrice(), 0.0);
        assertEquals(2.2, bookItemLayers.getOrderbookLayersList().get(1).getPrice(), 0.0);
        assertEquals(3.2, bookItemLayers.getOrderbookLayersList().get(2).getPrice(), 0.0);
    }

    @Test
    public void getOrderBookItemLayersReturnsAnEmptyItemOrderBookResponseWhenItemMarketDoesNotContainRequestItemName() throws InvalidProtocolBufferException {
        //Act
        ItemOrderBookResponse bookItemLayers = blockingStub.getOrderBookItemLayers(ITEM_ORDER_BOOK_REQUEST);

        //Assert
        assertEquals(0, bookItemLayers.getOrderbookLayersList().size());
    }

    @Test
    public void announceItemInterestExecutesSuccessfullyWithValidInterestRequest() throws IOException, TrackingException {
        //Arrange
        Set<String> interestsSet = mock(Set.class);
        when(interestsMap.get(CLIENT)).thenReturn(interestsSet);

        //Act
        InterestsResponse interestsResponse = blockingStub.announceItemInterest(AURORA_ANNOUNCE_REQUEST);

        //Assert
        assertTrue(interestsResponse.isInitialized());
        verify(interestsMap, times(1)).putIfAbsent(CLIENT, new HashSet<>());
        verify(interestsMap, times(1)).get(CLIENT);
        verify(interestsSet, times(1)).add(EGGS_ITEM_NAME);
        verify(interestsPersistence, times(1)).persistInterests();
        verify(auroraClient, times(1)).startSubscription(EGGS_ITEM_NAME, CLIENT);
    }

    @Test(expected = StatusRuntimeException.class)
    public void announceItemInterestThrowsStatusRuntimeExceptionWhenAuroraClientThrowsTrackingException() throws TrackingException {
        //Arrange
        when(interestsMap.get(CLIENT)).thenReturn(mock(Set.class));
        doThrow(new TrackingException(EXCEPTION_MESSAGE)).when(auroraClient).startSubscription(any(), any());

        //Act
        blockingStub.announceItemInterest(AURORA_ANNOUNCE_REQUEST);
    }

    @Test
    public void cancelItemSubscriptionExecutesSuccessfullyWithValidCancelSubscriptionRequest() throws IOException {
        //Act
        Map<String, Set<String>> interestsMap = new HashMap<>();
        interestsMap.put(CLIENT, new HashSet<>(Collections.singletonList(EGGS_ITEM_NAME)));
        ReflectionTestUtils.setField(orderBookService, "interestsMap", interestsMap);
        CancelSubscriptionResponse cancelSubscriptionResponse = blockingStub.cancelItemSubscription(CANCEL_SUBSCRIPTION_REQUEST);

        //Assert
        assertTrue(cancelSubscriptionResponse.isInitialized());
        assertFalse(interestsMap.containsKey(EGGS_ITEM_NAME));
        verify(interestsPersistence, times(1)).persistInterests();
    }

    @Test(expected = StatusRuntimeException.class)
    public void cancelItemSubscriptionThrowsStatusRuntimeExceptionWhenAuroraClientThrowsTrackingException() throws TrackingException {
        //Arrange
        Map<String, Set<String>> interestsMap = new HashMap<>();
        interestsMap.put(CLIENT, new HashSet<>(Collections.singletonList("eggs")));
        ReflectionTestUtils.setField(orderBookService, "interestsMap", interestsMap);
        doThrow(new TrackingException(EXCEPTION_MESSAGE)).when(auroraClient).stopSubscription(any(), any());

        //Act
        blockingStub.cancelItemSubscription(CANCEL_SUBSCRIPTION_REQUEST);
    }

    private TreeSet<Item> populateItems() {

        TreeSet<Item> items = new TreeSet<>();
        items.add(new Item(1.2, 3, Origin.EUROPE));
        items.add(new Item(2.2, 1, Origin.EUROPE));
        items.add(new Item(3.2, 2, Origin.EUROPE));
        return items;
    }

    private void populateList(Set<Item> items) {
        for (Item item : items) {
            OrderBookLayer build = OrderBookLayer.newBuilder().setQuantity(item.getQuantity()).setPrice(item.getPrice()).setOrigin(item.getOrigin()).build();
            orderBookLayers.add(build);
        }
    }

}