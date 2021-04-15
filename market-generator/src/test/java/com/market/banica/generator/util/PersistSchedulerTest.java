package com.market.banica.generator.util;

import com.market.banica.generator.model.MarketTick;
import com.market.banica.generator.task.SnapshotPersistenceTask;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PersistSchedulerTest {

    private static final Timer persistTimer = mock(Timer.class);
    private static final ReadWriteLock marketStateLock = mock(ReadWriteLock.class);
    private static final SnapshotPersistence snapshotPersistence = mock(SnapshotPersistence.class);
    @SuppressWarnings("unchecked")
    private static final Map<String, Set<MarketTick>> marketState = mock(Map.class);
    @SuppressWarnings("unchecked")
    private static final Queue<MarketTick> marketSnapshot = mock(Queue.class);
    private static final SnapshotPersistenceTask currentSnapshotPersistenceTask = mock(SnapshotPersistenceTask.class);

    private static PersistScheduler persistScheduler;

    @BeforeAll
    static void beforeAll() {

        persistScheduler = new PersistScheduler(marketStateLock, snapshotPersistence, marketState, marketSnapshot);
        ReflectionTestUtils.setField(persistScheduler, "persistTimer", persistTimer);
        ReflectionTestUtils.setField(persistScheduler, "currentSnapshotPersistenceTask",
                currentSnapshotPersistenceTask);

    }

    @BeforeEach
    @SuppressWarnings("unchecked")
    void resetInvocations() {

        reset(persistTimer);
        reset(marketStateLock);
        reset(snapshotPersistence);
        reset(marketState);
        reset(marketSnapshot);
        reset(currentSnapshotPersistenceTask);

    }

    @Test
    void setFrequency_ValidFrequency() {

        int newFrequency = 55;
        PersistScheduler persistSchedulerSpy = spy(persistScheduler);
        ReflectionTestUtils.setField(persistSchedulerSpy, "currentSnapshotPersistenceTask",
                currentSnapshotPersistenceTask);

        persistSchedulerSpy.setFrequency(newFrequency);

        assertEquals(newFrequency, ReflectionTestUtils.getField(persistSchedulerSpy, "frequencySchedule"));
        verify(currentSnapshotPersistenceTask, times(1)).cancel();
        verify(persistSchedulerSpy, times(1)).scheduleSnapshot();

    }

    @Test
    void setFrequency_OldFrequency() {

        int newFrequency = 60;
        PersistScheduler persistSchedulerSpy = spy(persistScheduler);
        ReflectionTestUtils.setField(persistSchedulerSpy, "currentSnapshotPersistenceTask",
                currentSnapshotPersistenceTask);

        assertThrows(IllegalArgumentException.class, () -> persistSchedulerSpy.setFrequency(newFrequency));

        verify(currentSnapshotPersistenceTask, times(0)).cancel();
        verify(persistSchedulerSpy, times(0)).scheduleSnapshot();

    }

    @Test
    void setFrequency_NonPositiveFrequency() {

        int newFrequency = -5;
        PersistScheduler persistSchedulerSpy = spy(persistScheduler);
        ReflectionTestUtils.setField(persistSchedulerSpy, "currentSnapshotPersistenceTask",
                currentSnapshotPersistenceTask);

        assertThrows(IllegalArgumentException.class, () -> persistSchedulerSpy.setFrequency(newFrequency));

        verify(currentSnapshotPersistenceTask, times(0)).cancel();
        verify(persistSchedulerSpy, times(0)).scheduleSnapshot();

    }

    @Test
    void scheduleSnapshot() {

        Integer frequencySchedule = (Integer) ReflectionTestUtils
                .getField(persistScheduler, "frequencySchedule");
        int frequencyScheduleInt = frequencySchedule != null ? frequencySchedule : -1;


        persistScheduler.scheduleSnapshot();


        SnapshotPersistenceTask newPersistenceTask = (SnapshotPersistenceTask) ReflectionTestUtils
                .getField(persistScheduler, "currentSnapshotPersistenceTask");

        assertEquals(new SnapshotPersistenceTask(marketStateLock, snapshotPersistence, marketState, marketSnapshot)
                , newPersistenceTask);

        verify(persistTimer).scheduleAtFixedRate(newPersistenceTask,
                TimeUnit.SECONDS.toMillis(frequencyScheduleInt),
                TimeUnit.SECONDS.toMillis(frequencyScheduleInt));

    }

}
