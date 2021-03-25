package com.market.banica.order.book.service.grpc;

import com.market.Origin;
import com.market.banica.common.ChannelRPCConfig;
import com.market.banica.order.book.exception.TrackingException;
import com.market.banica.order.book.model.Item;
import com.market.banica.order.book.model.ItemMarket;
import io.grpc.Context;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class AuroraClientTest {
    private static final String EGGS_ITEM_NAME = "eggs";
    private static final String RICE_ITEM_NAME = "rice";
    private static final String MEAT_ITEM_NAME = "meat";

    private static final String CLIENT = "client";

    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 9090;

    ManagedChannel managedChannel = ManagedChannelBuilder.forAddress(DEFAULT_HOST, DEFAULT_PORT)
            .usePlaintext()
            .defaultServiceConfig(ChannelRPCConfig.getInstance().getServiceConfig())
            .enableRetry()
            .build();

    private final ItemMarket itemMarket = new ItemMarket();
    private final AuroraClient auroraClient = new AuroraClient(itemMarket, DEFAULT_HOST, DEFAULT_PORT);

    private final Map<String, TreeSet<Item>> allItems = new ConcurrentHashMap<>();
    private final Map<String, Context.CancellableContext> cancellableStubs = new ConcurrentHashMap<>();

    @BeforeEach
    public void setUp() {
        TreeSet<Item> items = new TreeSet<>();
        items.add(new Item(1.2, 3, Origin.EUROPE));
        items.add(new Item(2.2, 1, Origin.EUROPE));
        items.add(new Item(3.2, 2, Origin.EUROPE));
        allItems.put(EGGS_ITEM_NAME, items);
        allItems.put(RICE_ITEM_NAME, new TreeSet<>());

        ReflectionTestUtils.setField(itemMarket, "allItems", allItems);
        ReflectionTestUtils.setField(auroraClient, "cancellableStubs", cancellableStubs);
        ReflectionTestUtils.setField(auroraClient, "managedChannel", managedChannel);
    }

    @Test
    public void updateItemsAddsItemsInAllItemsMapAndCancellableStubAsWell() throws TrackingException {
        //Arrange
        List<String> items = new ArrayList<>();
        items.add(EGGS_ITEM_NAME);
        items.add(RICE_ITEM_NAME);
        items.add(MEAT_ITEM_NAME);

        //Act
        this.auroraClient.updateItems(items, CLIENT);

        //Assert
        assertEquals(1, this.cancellableStubs.size());
        assertEquals(3, this.allItems.size());
        assertEquals(this.allItems.get(MEAT_ITEM_NAME).size(), 0);
    }

    @Test
    public void updateItemsWithExistingItemsThrowsTrackingException() {
        //Arrange
        this.cancellableStubs.put(MEAT_ITEM_NAME, Context.current().withCancellation());

        List<String> items = new ArrayList<>();
        items.add(EGGS_ITEM_NAME);
        items.add(RICE_ITEM_NAME);
        items.add(MEAT_ITEM_NAME);

        //Act, Assert
        assertThrows(TrackingException.class,
                () -> this.auroraClient.updateItems(items, CLIENT));
    }

    @Test
    public void stopTrackingItemsRemovesCancellableContextAndProductFromItemMarketWithSuccess() throws TrackingException {
        //Arrange
        this.cancellableStubs.put(RICE_ITEM_NAME, Context.current().withCancellation());

        List<String> items = new ArrayList<>();
        items.add(EGGS_ITEM_NAME);

        //Act
        this.auroraClient.updateItems(items, CLIENT);

        //Assert
        assertEquals(1, allItems.size());
        assertEquals(0, cancellableStubs.size());
    }

    @Test
    public void stopTrackingItemsThrowsTrackingException() throws TrackingException {
        //Arrange
        List<String> items = new ArrayList<>();
        items.add(EGGS_ITEM_NAME);

        //Act, Assert
        assertThrows(TrackingException.class, () -> this.auroraClient.updateItems(items, CLIENT));
    }

    @Test
    public void stopManagedChannelReturnsTrueAfterChannelCancellation() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        //Arrange
        Class<? extends AuroraClient> aClass = auroraClient.getClass();
        Method stop = aClass.getDeclaredMethod("stop");
        stop.setAccessible(true);

        //Act
        stop.invoke(auroraClient, null);
//        managedChannel.shutdownNow();

        //Assert
        assertTrue(managedChannel.isShutdown());
    }
}