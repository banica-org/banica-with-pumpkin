package com.market.banica.generator.util;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import com.market.banica.common.util.ApplicationDirectoryUtil;
import com.market.banica.generator.model.MarketTick;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SnapshotPersistenceTest {

    private static final Kryo kryoHandle = mock(Kryo.class);

    private static final String stateFileName = "test-marketState.dat";
    private static final String snapshotFileName = "test-marketSnapshot.dat";

    private static final String GOOD_NAME = "banica";

    private static SnapshotPersistence snapshotPersistence;

    @BeforeAll
    static void beforeAll() {

        snapshotPersistence = new SnapshotPersistence(stateFileName, snapshotFileName);

    }

    @AfterEach
    void teardown() throws IOException {

        reset(kryoHandle);
        File stateFile = ApplicationDirectoryUtil.getConfigFile(stateFileName);
        File snapshotFile = ApplicationDirectoryUtil.getConfigFile(snapshotFileName);
        assert stateFile.delete();
        assert snapshotFile.delete();

    }

    @Test
    void persistMarketState() throws IOException {

        SnapshotPersistence snapshotPersistence = new SnapshotPersistence(stateFileName, snapshotFileName);
        SnapshotPersistence snapshotPersistenceSpy = spy(snapshotPersistence);
        ReflectionTestUtils.setField(snapshotPersistenceSpy, "kryoHandle", kryoHandle);

        Map<String, Set<MarketTick>> marketState = new ConcurrentHashMap<>();
        Map<String, Set<MarketTick>> marketStateSpy = spy(marketState);

        Queue<MarketTick> marketSnapshot = new LinkedBlockingQueue<>();
        Queue<MarketTick> marketSnapshotSpy = spy(marketSnapshot);


        snapshotPersistenceSpy.persistMarketState(marketStateSpy, marketSnapshotSpy);


        verify(marketSnapshotSpy, times(1)).clear();
        assertTrue(marketSnapshot.isEmpty());

        verify(kryoHandle, times(1)).writeClassAndObject(any(Output.class), eq(marketStateSpy));
        verify(snapshotPersistenceSpy, times(1)).persistMarketSnapshot(marketSnapshotSpy);

    }

    @Test
    void persistMarketSnapshot() throws IOException {

        SnapshotPersistence snapshotPersistence = new SnapshotPersistence(stateFileName, snapshotFileName);
        ReflectionTestUtils.setField(snapshotPersistence, "kryoHandle", kryoHandle);
        Queue<MarketTick> marketSnapshot = new LinkedBlockingQueue<>();
        Queue<MarketTick> marketSnapshotSpy = spy(marketSnapshot);

        snapshotPersistence.persistMarketSnapshot(marketSnapshotSpy);

        verify(kryoHandle, times(1)).writeClassAndObject(any(Output.class), eq(marketSnapshotSpy));

    }

    @Test
    void loadMarketState_WhenFileDoesNotExist() throws IOException {

        assertFalse(ApplicationDirectoryUtil.doesFileExist(stateFileName));

        Map<String, Set<MarketTick>> result = snapshotPersistence.loadMarketState(new LinkedBlockingQueue<>());

        assertTrue(ApplicationDirectoryUtil.doesFileExist(stateFileName));
        assertEquals(new ConcurrentHashMap<>(), result);

    }

    @Test
    void loadMarketState_WhenFileExistsButIsEmpty() throws IOException {

        ApplicationDirectoryUtil.getConfigFile(stateFileName);
        assertTrue(ApplicationDirectoryUtil.doesFileExist(stateFileName));

        Map<String, Set<MarketTick>> result = snapshotPersistence.loadMarketState(new LinkedBlockingQueue<>());

        assertTrue(ApplicationDirectoryUtil.doesFileExist(stateFileName));
        assertEquals(new ConcurrentHashMap<>(), result);

    }

    @Test
    void loadMarketState_WhenFileExistsAndHasTicks() throws IOException {

        Queue<MarketTick> marketSnapshotQueue = new LinkedBlockingQueue<>();
        marketSnapshotQueue.add(new MarketTick(GOOD_NAME, 3, 3, 3));
        marketSnapshotQueue.add(new MarketTick(GOOD_NAME, 4, 4, 4));

        snapshotPersistence.persistMarketSnapshot(marketSnapshotQueue);

        Map<String, Set<MarketTick>> expected = new ConcurrentHashMap<>();
        expected.put(GOOD_NAME, new TreeSet<>());
        expected.get(GOOD_NAME).add(new MarketTick(GOOD_NAME, 1, 1, 1));
        expected.get(GOOD_NAME).add(new MarketTick(GOOD_NAME, 2, 2, 2));
        snapshotPersistence.persistMarketState(expected, new LinkedBlockingQueue<>());

        Map<String, Set<MarketTick>> actual = snapshotPersistence.loadMarketState(marketSnapshotQueue);

        expected.get(GOOD_NAME).add(new MarketTick(GOOD_NAME, 3, 3, 3));
        expected.get(GOOD_NAME).add(new MarketTick(GOOD_NAME, 4, 4, 4));

        assertEquals(expected, actual);

    }

    @Test
    void loadMarketSnapshot_WhenFileDoesNotExist() throws IOException {

        assertFalse(ApplicationDirectoryUtil.doesFileExist(snapshotFileName));

        Queue<MarketTick> result = snapshotPersistence.loadMarketSnapshot();

        assertTrue(ApplicationDirectoryUtil.doesFileExist(snapshotFileName));
        assertTrue(result.isEmpty());
        assertTrue(result instanceof LinkedBlockingQueue);

    }

    @Test
    void loadMarketSnapshot_WhenFileExistsButIsEmpty() throws IOException {

        ApplicationDirectoryUtil.getConfigFile(snapshotFileName);
        assertTrue(ApplicationDirectoryUtil.doesFileExist(snapshotFileName));

        Queue<MarketTick> result = snapshotPersistence.loadMarketSnapshot();

        assertTrue(ApplicationDirectoryUtil.doesFileExist(snapshotFileName));
        assertTrue(result.isEmpty());
        assertTrue(result instanceof LinkedBlockingQueue);

    }

    @Test
    void loadMarketSnapshot_WhenFileExistsAndHasTicks() throws IOException {

        Queue<MarketTick> expected = new LinkedBlockingQueue<>();
        expected.add(new MarketTick(GOOD_NAME, 1, 1, 1));
        expected.add(new MarketTick(GOOD_NAME, 2, 2, 2));
        snapshotPersistence.persistMarketSnapshot(expected);

        Queue<MarketTick> actual = snapshotPersistence.loadMarketSnapshot();

        assertEquals(expected.size(), actual.size());
        actual.forEach(marketTick -> assertEquals(expected.poll(), marketTick));

    }

}
