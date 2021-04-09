package com.market.banica.order.book.service.grpc;
import com.market.Origin;
import com.market.banica.common.channel.ChannelRPCConfig;
import com.market.banica.common.exceptions.TrackingException;
import com.market.banica.order.book.model.Item;
import com.market.banica.order.book.model.ItemMarket;
import io.grpc.Context;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AuroraClientTest {
    private static final String EGGS_ITEM_NAME = "eggs";
    private static final String RICE_ITEM_NAME = "rice";
    private static final String MEAT_ITEM_NAME = "meat";

    private static final String ALL_ITEMS_FIELD = "allItems";
    private static final String MANAGED_CHANNEL_FIELD = "managedChannel";
    private static final String CANCELLABLE_STUBS_FIELD = "cancellableStubs";

    private static final String CLIENT = "client";

    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 9090;

    private final ManagedChannel managedChannel = ManagedChannelBuilder.forAddress(DEFAULT_HOST, DEFAULT_PORT)
            .usePlaintext()
            .defaultServiceConfig(ChannelRPCConfig.getInstance().getServiceConfig())
            .enableRetry()
            .build();

    private final ItemMarket itemMarket = new ItemMarket();
    private final AuroraClient auroraClient = new AuroraClient(itemMarket, DEFAULT_HOST, DEFAULT_PORT);

    private final Map<String, TreeSet<Item>> allItems = new ConcurrentHashMap<>();
    private final Map<String, Context.CancellableContext> cancellableStubs = new ConcurrentHashMap<>();

    @Before
    public void setUp() {
        TreeSet<Item> items = this.populateItems();

        allItems.put(EGGS_ITEM_NAME, items);
        allItems.put(RICE_ITEM_NAME, new TreeSet<>());

        ReflectionTestUtils.setField(itemMarket, ALL_ITEMS_FIELD, allItems);
        ReflectionTestUtils.setField(auroraClient, CANCELLABLE_STUBS_FIELD, cancellableStubs);
        ReflectionTestUtils.setField(auroraClient, MANAGED_CHANNEL_FIELD, managedChannel);
    }

    @Test
    public void startSubscriptionAddsItemsInAllItemsMapAndCancellableStub() throws TrackingException {
        //Act
        this.auroraClient.startSubscription(EGGS_ITEM_NAME, CLIENT);
        this.auroraClient.startSubscription(RICE_ITEM_NAME, CLIENT);
        this.auroraClient.startSubscription(MEAT_ITEM_NAME, CLIENT);

        //Assert
        assertEquals(3, this.cancellableStubs.size());
        assertEquals(3, this.allItems.size());
        assertEquals(this.allItems.get(MEAT_ITEM_NAME).size(), 0);
    }

    @Test(expected = TrackingException.class)
    public void startSubscriptionWithExistingItemsThrowsTrackingException() throws TrackingException {
        //Arrange
        this.cancellableStubs.put(MEAT_ITEM_NAME, Context.current().withCancellation());

        //Act
        this.auroraClient.startSubscription(MEAT_ITEM_NAME, CLIENT);
    }

    @Test
    public void stopSubscriptionRemovesCancellableContextAndProductFromItemMarketWithSuccess() throws TrackingException {
        //Arrange
        this.cancellableStubs.put(RICE_ITEM_NAME, Context.current().withCancellation());

        //Act
        this.auroraClient.stopSubscription(RICE_ITEM_NAME, CLIENT);

        //Assert
        assertEquals(1, allItems.size());
        assertEquals(0, cancellableStubs.size());
    }

    @Test(expected = TrackingException.class)
    public void stopSubscriptionThrowsTrackingException() throws TrackingException {
        //Act
        this.auroraClient.stopSubscription(EGGS_ITEM_NAME, CLIENT);
    }

    @Test
    public void stopManagedChannelReturnsTrueAfterChannelCancellation() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        //Arrange
        Class<? extends AuroraClient> aClass = auroraClient.getClass();
        Method stop = aClass.getDeclaredMethod("stop");
        stop.setAccessible(true);

        //Act
        stop.invoke(auroraClient, null);

        //Assert
        assertTrue(managedChannel.isShutdown());
    }

    private TreeSet<Item> populateItems() {
        TreeSet<Item> items = new TreeSet<>();
        items.add(new Item(1.2, 3, Origin.EUROPE));
        items.add(new Item(2.2, 1, Origin.EUROPE));
        items.add(new Item(3.2, 2, Origin.EUROPE));
        return items;
    }
}