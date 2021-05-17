package com.market.banica.generator.service;


import com.google.common.util.concurrent.MoreExecutors;
import com.market.TickResponse;
import com.market.banica.common.exception.ProductNotAvailableException;
import com.market.banica.common.util.ApplicationDirectoryUtil;
import com.market.banica.generator.model.MarketTick;
import com.market.banica.generator.util.PersistScheduler;
import com.market.banica.generator.util.SnapshotPersistence;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketStateImplTest {

    private static final String GOOD_BANICA = "banica";
    private static final String GOOD_EGGS = "eggs";

    private static final MarketSubscriptionManager subscriptionManager = mock(MarketSubscriptionManager.class);
    @SuppressWarnings("unchecked")
    private static final Map<String, Set<MarketTick>> marketStateMap = mock(Map.class);
    @SuppressWarnings("unchecked")
    private static final Queue<MarketTick> marketSnapshot = mock(Queue.class);
    private static final PersistScheduler persistScheduler = mock(PersistScheduler.class);
    private static final SnapshotPersistence snapshotPersistence = mock(SnapshotPersistence.class);

    private static final String stateFileName = "test-marketState.dat";
    private static final String snapshotFileName = "test-marketSnapshot.dat";

    private static MarketState marketState;

    @BeforeAll
    static void beforeAll() throws IOException {

        marketState = new MarketStateImpl(stateFileName, snapshotFileName, subscriptionManager);
        ReflectionTestUtils.setField(marketState, "marketState", marketStateMap);
        ReflectionTestUtils.setField(marketState, "marketSnapshot", marketSnapshot);
        ReflectionTestUtils.setField(marketState, "executorService", MoreExecutors.newDirectExecutorService());
        ReflectionTestUtils.setField(marketState, "persistScheduler", persistScheduler);
        ReflectionTestUtils.setField(marketState, "snapshotPersistence", snapshotPersistence);
        MarketTick.setOrigin("europe");


    }

    @AfterAll
    static void afterAll() throws IOException {

        File testStateFile = ApplicationDirectoryUtil.getConfigFile(stateFileName);
        File testSnapshotFile = ApplicationDirectoryUtil.getConfigFile(snapshotFileName);
        testStateFile.deleteOnExit();
        testSnapshotFile.deleteOnExit();

    }

    @BeforeEach
    @SuppressWarnings("unchecked")
    void resetInvocations() {

        reset(subscriptionManager);
        reset(marketStateMap);
        reset(marketSnapshot);
        reset(persistScheduler);
        reset(snapshotPersistence);

    }

    @Test
    void addTickToMarketSnapshot() throws IOException {

        MarketTick marketTick = mock(MarketTick.class);
        TreeSet<MarketTick> emptySet = new TreeSet<>();
        TreeSet<MarketTick> emptySetSpy = spy(emptySet);

        when(marketTick.getGood()).thenReturn(GOOD_BANICA);
        when(marketStateMap.containsKey(GOOD_BANICA)).thenReturn(false);
        when(marketStateMap.put(GOOD_BANICA, emptySet)).thenReturn(emptySet);
        when(marketStateMap.get(GOOD_BANICA)).thenReturn(emptySetSpy);


        marketState.addTickToMarket(marketTick);


        verify(marketSnapshot, times(1)).add(marketTick);
        verify(marketStateMap, times(1)).putIfAbsent(GOOD_BANICA, new TreeSet<>());
        verify(emptySetSpy, times(1)).add(marketTick);
        verify(snapshotPersistence, times(1)).persistMarketSnapshot(marketSnapshot);
        verify(subscriptionManager, times(1)).notifySubscribers(convertMarketTickToTickResponse(marketTick));

    }

    @Test
    void generateMarketTicks() {

        MarketTick marketTick1 = new MarketTick(GOOD_BANICA, 1, 1, 1);
        MarketTick marketTick2 = new MarketTick(GOOD_BANICA, 2, 2, 2);
        MarketTick marketTick3 = new MarketTick(GOOD_EGGS, 3, 3, 3);
        MarketTick marketTick4 = new MarketTick(GOOD_BANICA, 4, 4, 4);

        Set<MarketTick> marketTicks = new TreeSet<>(Arrays.asList(marketTick1, marketTick2, marketTick3, marketTick4));

        when(marketStateMap.getOrDefault(GOOD_BANICA, new TreeSet<>()))
                .thenReturn(marketTicks);

        List<TickResponse> actual = Arrays.asList(convertMarketTickToTickResponse(marketTick1),
                convertMarketTickToTickResponse(marketTick2),
                convertMarketTickToTickResponse(marketTick3),
                convertMarketTickToTickResponse(marketTick4));


        List<TickResponse> result = marketState.generateMarketTicks(GOOD_BANICA);


        assertEquals(actual, result);
        verify(marketStateMap, times(1)).getOrDefault(GOOD_BANICA, new TreeSet<>());

    }

    @Test
    public void removeItemFromStateWithValidRequestRemovesRequiredMarketTicksAmount() throws ProductNotAvailableException {

        MarketTick marketTick1 = new MarketTick(GOOD_EGGS, 2, 1, 1);
        MarketTick marketTick2 = new MarketTick(GOOD_EGGS, 2, 1, 2);
        MarketTick marketTick3 = new MarketTick(GOOD_EGGS, 7, 1, 3);

        Set<MarketTick> marketTicks = new TreeSet<>(Arrays.asList(marketTick1, marketTick2, marketTick3));

        when(marketStateMap.get(GOOD_EGGS)).thenReturn(marketTicks);

        marketState.removeItemFromState(GOOD_EGGS, 8, 1);

        TreeSet<MarketTick> resultMap = (TreeSet<MarketTick>) marketStateMap.get(GOOD_EGGS);
        long marketTicksQuantitySum = resultMap.stream().mapToLong(MarketTick::getQuantity).sum();

        assertEquals(4, resultMap.size());
        assertEquals(3, marketTicksQuantitySum);

    }

    @Test
    public void removeItemFromStateWithValidRequestDecreasesRequiredNumberOfMarketTickQuantity() throws ProductNotAvailableException {

        MarketTick marketTick1 = new MarketTick(GOOD_EGGS, 7, 1, 1);
        MarketTick marketTick2 = new MarketTick(GOOD_EGGS, 8, 1, 2);

        Set<MarketTick> marketTicks = new TreeSet<>(Arrays.asList(marketTick1, marketTick2));

        when(marketStateMap.get(GOOD_EGGS)).thenReturn(marketTicks);

        marketState.removeItemFromState(GOOD_EGGS, 5, 1);

        Set<MarketTick> resultMap = marketStateMap.get(GOOD_EGGS);
        long marketTicksQuantitySum = resultMap.stream().mapToLong(MarketTick::getQuantity).sum();

        assertEquals(3, resultMap.size());
        assertEquals(10, marketTicksQuantitySum);

    }

    @Test
    public void removeItemFromStateWithValidRequestDecreasesMarketTicksQuantity() throws ProductNotAvailableException {

        MarketTick marketTick1 = new MarketTick(GOOD_EGGS, 2, 1, 1);
        MarketTick marketTick2 = new MarketTick(GOOD_EGGS, 2, 1, 2);

        Set<MarketTick> marketTicks = new TreeSet<>(Arrays.asList(marketTick1, marketTick2));

        when(marketStateMap.get(GOOD_EGGS)).thenReturn(marketTicks);

        marketState.removeItemFromState(GOOD_EGGS, 4, 1);

        Set<MarketTick> resultMap = marketStateMap.get(GOOD_EGGS);
        long marketTicksQuantitySum = resultMap.stream().mapToLong(MarketTick::getQuantity).sum();

        assertEquals(3, resultMap.size());
        assertEquals(0, marketTicksQuantitySum);

    }

    @Test
    public void removeItemFromStateThrowsExceptionWhenRequestedItemIsNotAvailableOnMarket() {

        when(marketStateMap.get(GOOD_EGGS)).thenReturn(Collections.emptySet());

        assertThrows(ProductNotAvailableException.class, () -> marketState.removeItemFromState(GOOD_EGGS, 8, 1));

    }

    @Test
    public void removeItemFromStateThrowsExceptionWhenQuantityOfRequestedItemIsGreaterThanAvailableMarketTicksQuantity() {

        MarketTick marketTick1 = new MarketTick(GOOD_EGGS, 7, 1, 1);

        Set<MarketTick> marketTicks = new TreeSet<>(Collections.singletonList(marketTick1));

        when(marketStateMap.get(GOOD_EGGS)).thenReturn(marketTicks);

        assertThrows(ProductNotAvailableException.class, () -> marketState.removeItemFromState(GOOD_EGGS, 8, 1));

    }

    @Test
    public void addGoodToStateCreatesNewTreeSetWhenMarketStateDoesNotContainMarketTickWithGivenName() {

        long quantity = 5;
        double price = 1.0;

        when(marketStateMap.get(GOOD_EGGS)).thenReturn(new TreeSet<>());

        marketState.addProductToMarketState(GOOD_EGGS, price, quantity);

        TreeSet<MarketTick> expectedMarketTicks = (TreeSet<MarketTick>) marketStateMap.get(GOOD_EGGS);

        assertEquals(1, expectedMarketTicks.size());

    }

    @Test
    public void addGoodToStateAddsSecondMarketTickWhenMarketStateInNotEmpty() {

        MarketTick firstMarketTick = new MarketTick(GOOD_EGGS, 7, 2, 1);

        Set<MarketTick> marketTickSet = new TreeSet<>();
        marketTickSet.add(firstMarketTick);

        when(marketStateMap.get(GOOD_EGGS)).thenReturn(marketTickSet);

        marketState.addProductToMarketState(GOOD_EGGS, 1, 8);

        TreeSet<MarketTick> expectedMarketTicks = (TreeSet<MarketTick>) marketStateMap.get(GOOD_EGGS);

        MarketTick availableMarketTick = expectedMarketTicks.first();

        assertEquals(2, expectedMarketTicks.size());
        assertEquals(7, availableMarketTick.getQuantity());

    }

    @Test
    void getPersistScheduler() {

        assertEquals(persistScheduler, marketState.getPersistScheduler());

    }

    private TickResponse convertMarketTickToTickResponse(MarketTick marketTick) {
        return TickResponse.newBuilder()
                .setOrigin(MarketTick.getOrigin())
                .setTimestamp(marketTick.getTimestamp())
                .setGoodName(marketTick.getGood())
                .setPrice(marketTick.getPrice())
                .setQuantity(marketTick.getQuantity())
                .build();
    }

}
