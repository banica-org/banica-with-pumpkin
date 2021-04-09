package com.market.banica.generator.task;

import com.market.banica.generator.model.MarketTick;
import com.market.banica.generator.util.SnapshotPersistence;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SnapshotPersistenceTaskTest {

    private static final ReadWriteLock marketStateLock = mock(ReadWriteLock.class);
    private static final SnapshotPersistence snapshotPersistence = mock(SnapshotPersistence.class);
    @SuppressWarnings("unchecked")
    private static final Map<String, Set<MarketTick>> newTicks = mock(Map.class);

    private static SnapshotPersistenceTask snapshotPersistenceTask;

    @BeforeAll
    static void beforeAll() {

        snapshotPersistenceTask = new SnapshotPersistenceTask(marketStateLock, snapshotPersistence, newTicks);

    }

    @Test
    void run() throws IOException {

        Lock newTicksReadLock = mock(ReentrantReadWriteLock.ReadLock.class);
        when(marketStateLock.readLock()).thenReturn(newTicksReadLock);

        snapshotPersistenceTask.run();

        verify(newTicksReadLock, times(1)).lock();
        verify(snapshotPersistence, times(1)).persistTicks(newTicks);
        verify(newTicksReadLock, times(1)).unlock();

    }

}
