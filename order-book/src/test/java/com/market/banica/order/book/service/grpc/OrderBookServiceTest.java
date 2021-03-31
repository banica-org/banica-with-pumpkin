package com.market.banica.order.book.service.grpc;

import com.market.Origin;
import com.market.banica.order.book.exception.TrackingException;
import com.market.banica.order.book.model.Item;
import com.market.banica.order.book.model.ItemMarket;
import com.orderbook.CancelSubscriptionRequest;
import com.orderbook.CancelSubscriptionResponse;
import com.orderbook.InterestsRequest;
import com.orderbook.InterestsResponse;
import com.orderbook.ItemOrderBookRequest;
import com.orderbook.ItemOrderBookResponse;
import com.orderbook.OrderBookServiceGrpc;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OrderBookServiceTest {
    private static final String EGGS_ITEM_NAME = "eggs";

    private static final String EXCEPTION_MESSAGE = "Mock method to throw exception.";

    private static final String CLIENT = "europe";

    private static final ItemOrderBookRequest ITEM_ORDER_BOOK_REQUEST = ItemOrderBookRequest.newBuilder()
            .setItemName(EGGS_ITEM_NAME)
            .setClientId(CLIENT)
            .setQuantity(2)
            .build();

    private static final InterestsRequest INTERESTS_REQUEST =
            InterestsRequest.newBuilder().setItemName(EGGS_ITEM_NAME).setClientId(CLIENT).build();

    private static final CancelSubscriptionRequest CANCEL_SUBSCRIPTION_REQUEST =
            CancelSubscriptionRequest.newBuilder().setClientId(EGGS_ITEM_NAME).setClientId(CLIENT).build();


    private Set<Item> items;
    private OrderBookServiceGrpc.OrderBookServiceBlockingStub blockingStub;

    @Rule
    public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    @Mock
    private ItemMarket itemMarket;

    @Mock
    private AuroraClient auroraClient;

    @InjectMocks
    private OrderBookService orderBookService;


    @SneakyThrows
    @Before
    public void setUp() {
        items = this.populateItems();

        String serverName = InProcessServerBuilder.generateName();

        grpcCleanup.register(InProcessServerBuilder
                .forName(serverName).directExecutor().addService(orderBookService).build().start());

        blockingStub = OrderBookServiceGrpc.newBlockingStub(
                grpcCleanup.register(InProcessChannelBuilder.forName(serverName).directExecutor().build()));
    }

    @Test
    public void getOrderBookItemLayersSuccessfullyReturnsItemOrderBookResponseWhenItemMarketContainsRequestItemName() {
        //Arrange
        when(itemMarket.getItemSetByName(any())).thenReturn(Optional.of(this.items));

        //Act
        ItemOrderBookResponse bookItemLayers = blockingStub.getOrderBookItemLayers(ITEM_ORDER_BOOK_REQUEST);

        //Assert
        assertEquals(3, bookItemLayers.getOrderbookLayersList().size());
        assertEquals(1.2, bookItemLayers.getOrderbookLayersList().get(0).getPrice(), 0.0);
        assertEquals(2.2, bookItemLayers.getOrderbookLayersList().get(1).getPrice(), 0.0);
        assertEquals(3.2, bookItemLayers.getOrderbookLayersList().get(2).getPrice(), 0.0);
    }

    @Test
    public void getOrderBookItemLayersReturnsAnEmptyItemOrderBookResponseWhenItemMarketDoesNotContainRequestItemName() {
        //Arrange
        when(itemMarket.getItemSetByName(any())).thenReturn(Optional.empty());

        //Act
        ItemOrderBookResponse bookItemLayers = blockingStub.getOrderBookItemLayers(ITEM_ORDER_BOOK_REQUEST);

        //Assert
        assertEquals(0, bookItemLayers.getOrderbookLayersList().size());
    }

    @Test
    public void announceItemInterestExecutesSuccessfullyWithValidInterestRequest() {
        //Act
        InterestsResponse interestsResponse = blockingStub.announceItemInterest(INTERESTS_REQUEST);

        //Assert
        assertTrue(interestsResponse.isInitialized());
    }

    @Test(expected = StatusRuntimeException.class)
    public void announceItemInterestThrowsStatusRuntimeExceptionWhenAuroraClientThrowsTrackingException() throws TrackingException {
        //Arrange
        doThrow(new TrackingException(EXCEPTION_MESSAGE)).when(auroraClient).startSubscription(any(), any());

        //Act
        blockingStub.announceItemInterest(INTERESTS_REQUEST);
    }

    @Test
    public void cancelItemSubscriptionExecutesSuccessfullyWithValidCancelSubscriptionRequest() {
        //Act
        CancelSubscriptionResponse cancelSubscriptionResponse = blockingStub.cancelItemSubscription(CANCEL_SUBSCRIPTION_REQUEST);

        //Assert
        assertTrue(cancelSubscriptionResponse.isInitialized());
    }

    @Test(expected = StatusRuntimeException.class)
    public void cancelItemSubscriptionThrowsStatusRuntimeExceptionWhenAuroraClientThrowsTrackingException() throws TrackingException {
        //Arrange
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
}