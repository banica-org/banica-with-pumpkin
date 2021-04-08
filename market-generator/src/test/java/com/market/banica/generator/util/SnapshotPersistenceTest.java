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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SnapshotPersistenceTest {

    private static final Kryo kryoHandle = mock(Kryo.class);

    private static final String fileName = "test-tickDatabase.dat";

    private static final String GOOD_NAME = "banica";

    private static SnapshotPersistence snapshotPersistence;

    @BeforeAll
    static void beforeAll() {

        snapshotPersistence = new SnapshotPersistence(fileName);

    }

    @AfterEach
    void teardown() throws IOException {

        File testTickDB = ApplicationDirectoryUtil.getConfigFile(fileName);
        assert testTickDB.delete();

    }

    @Test
    void persistTicks() throws FileNotFoundException {

        SnapshotPersistence snapshotPersistence = new SnapshotPersistence(fileName);
        ReflectionTestUtils.setField(snapshotPersistence, "kryoHandle", kryoHandle);
        Map<String, Set<MarketTick>> newTicks = new ConcurrentHashMap<>();
        Map<String, Set<MarketTick>> newTicksSpy = spy(newTicks);

        snapshotPersistence.persistTicks(newTicksSpy);

        verify(kryoHandle, times(1)).writeClassAndObject(any(Output.class), eq(newTicksSpy));

    }

    @Test
    void loadPersistedSnapshot_WhenFileDoesNotExist() throws IOException {

        assertFalse(ApplicationDirectoryUtil.doesFileExist(fileName));

        Map<String, Set<MarketTick>> result = snapshotPersistence.loadPersistedSnapshot();

        assertTrue(ApplicationDirectoryUtil.doesFileExist(fileName));
        assertEquals(new ConcurrentHashMap<>(), result);

    }

    @Test
    void loadPersistedSnapshot_WhenFileExistsButIsEmpty() throws IOException {

        ApplicationDirectoryUtil.getConfigFile(fileName);
        assertTrue(ApplicationDirectoryUtil.doesFileExist(fileName));

        Map<String, Set<MarketTick>> result = snapshotPersistence.loadPersistedSnapshot();

        assertTrue(ApplicationDirectoryUtil.doesFileExist(fileName));
        assertEquals(new ConcurrentHashMap<>(), result);

    }

    @Test
    void loadPersistedSnapshot_WhenFileExistsAndHasTicks() throws IOException {

        ApplicationDirectoryUtil.getConfigFile(fileName);
        Map<String, Set<MarketTick>> expected = new ConcurrentHashMap<>();
        expected.put(GOOD_NAME, new TreeSet<>());
        expected.get(GOOD_NAME).add(new MarketTick(GOOD_NAME, 1, 1, 1));
        expected.get(GOOD_NAME).add(new MarketTick(GOOD_NAME, 2, 2, 2));
        snapshotPersistence.persistTicks(expected);

        Map<String, Set<MarketTick>> actual = snapshotPersistence.loadPersistedSnapshot();

        assertEquals(expected, actual);

    }

}
