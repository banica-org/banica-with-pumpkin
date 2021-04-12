package com.market.banica.generator.service;


import com.google.common.util.concurrent.MoreExecutors;
import com.market.TickResponse;
import com.market.banica.common.util.ApplicationDirectoryUtil;
import com.market.banica.generator.model.MarketTick;
import com.market.banica.generator.util.PersistScheduler;
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
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketStateImplTest {

    private static final String GOOD_BANICA = "banica";

    private static final MarketSubscriptionManager subscriptionManager = mock(MarketSubscriptionManager.class);
    @SuppressWarnings("unchecked")
    private static final Map<String, Set<MarketTick>> marketStateMap = mock(Map.class);
    private static final PersistScheduler persistScheduler = mock(PersistScheduler.class);

    private static final String fileName = "test-tickDatabase.dat";

    private static MarketState marketState;

    @BeforeAll
    static void beforeAll() throws IOException {

        marketState = new MarketStateImpl(fileName, subscriptionManager);
        ReflectionTestUtils.setField(marketState, "marketState", marketStateMap);
        ReflectionTestUtils.setField(marketState, "executorService", MoreExecutors.newDirectExecutorService());
        ReflectionTestUtils.setField(marketState, "persistScheduler", persistScheduler);
        MarketTick.setOrigin("europe");

    }

    @AfterAll
    static void afterAll() throws IOException {

        File testTickDB = ApplicationDirectoryUtil.getConfigFile(fileName);
        testTickDB.deleteOnExit();

    }

    @BeforeEach
    @SuppressWarnings("unchecked")
    void resetInvocations() {

        reset(subscriptionManager);
        reset(marketStateMap);
        reset(persistScheduler);

    }

    @Test
    void addTickToMarketState_CratesNewSet() {

        MarketTick marketTick = mock(MarketTick.class);
        TreeSet<MarketTick> emptySet = new TreeSet<>();
        TreeSet<MarketTick> emptySetSpy = spy(emptySet);

        when(marketTick.getGood()).thenReturn(GOOD_BANICA);
        when(marketStateMap.containsKey(GOOD_BANICA)).thenReturn(false);
        when(marketStateMap.put(GOOD_BANICA, emptySet)).thenReturn(emptySet);
        when(marketStateMap.get(GOOD_BANICA)).thenReturn(emptySetSpy);


        marketState.addTickToMarketState(marketTick);


        verify(marketStateMap, times(1)).containsKey(GOOD_BANICA);
        verify(marketStateMap, times(1)).put(GOOD_BANICA, new TreeSet<>());
        verify(emptySetSpy, times(1)).add(marketTick);
        verify(subscriptionManager, times(1))
                .notifySubscribers(convertMarketTickToTickResponse(marketTick));

    }

    @Test
    void addTickToMarketState_UsesOldSet() {

        MarketTick marketTick = mock(MarketTick.class);
        TreeSet<MarketTick> oldSet = new TreeSet<>();
        TreeSet<MarketTick> oldSetSpy = spy(oldSet);

        when(marketTick.getGood()).thenReturn(GOOD_BANICA);
        when(marketStateMap.containsKey(GOOD_BANICA)).thenReturn(true);
        when(marketStateMap.get(GOOD_BANICA)).thenReturn(oldSetSpy);


        marketState.addTickToMarketState(marketTick);


        verify(marketStateMap, times(1)).containsKey(GOOD_BANICA);
        verify(oldSetSpy, times(1)).add(marketTick);
        verify(subscriptionManager, times(1))
                .notifySubscribers(convertMarketTickToTickResponse(marketTick));

    }

    @Test
    void generateMarketTicks_WhenGoodDoesNotExist() {

        when(marketStateMap.containsKey(GOOD_BANICA)).thenReturn(false);

        List<TickResponse> result = marketState.generateMarketTicks(GOOD_BANICA);

        assertEquals(Collections.emptyList(), result);
        verify(marketStateMap, times(1)).containsKey(GOOD_BANICA);

    }

    @Test
    void generateMarketTicks_WhenGoodExists() {

        MarketTick marketTick1 = new MarketTick(GOOD_BANICA, 1, 1, 1);
        MarketTick marketTick2 = new MarketTick(GOOD_BANICA, 2, 2, 2);

        Set<MarketTick> marketTicks = new TreeSet<>(Arrays.asList(marketTick1, marketTick2));

        when(marketStateMap.containsKey(GOOD_BANICA)).thenReturn(true);
        when(marketStateMap.get(GOOD_BANICA)).thenReturn(marketTicks);
        List<TickResponse> actual = Arrays.asList(convertMarketTickToTickResponse(marketTick1),
                convertMarketTickToTickResponse(marketTick2));


        List<TickResponse> result = marketState.generateMarketTicks(GOOD_BANICA);


        assertEquals(actual, result);
        verify(marketStateMap, times(1)).containsKey(GOOD_BANICA);
        verify(marketStateMap, times(1)).get(GOOD_BANICA);

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
