package com.market.banica.generator.service;

import com.market.banica.generator.model.MarketTick;
import com.market.banica.generator.service.task.SnapshotPersistenceTask;
import com.market.banica.generator.util.SnapshotPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;

@ManagedResource
public class PersistSchedulerImpl implements PersistScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PersistSchedulerImpl.class);

    private static final Timer PERSIST_TIMER = new Timer();

    private int frequencySchedule = 60;
    private SnapshotPersistenceTask currentSnapshotPersistenceTask;

    private final ReadWriteLock marketStateLock;
    private final SnapshotPersistence snapshotPersistence;
    private final Map<String, Set<MarketTick>> newTicks;

    public PersistSchedulerImpl(ReadWriteLock marketStateLock,
                                SnapshotPersistence snapshotPersistence,
                                Map<String, Set<MarketTick>> newTicks) {
        this.marketStateLock = marketStateLock;
        this.snapshotPersistence = snapshotPersistence;
        this.newTicks = newTicks;
    }

    @Override
    @ManagedOperation
    public void setFrequency(int frequency) {
        if (this.frequencySchedule != frequency) {
            if (frequency > 0) {

                LOGGER.info("Changing the snapshot frequency to {}!", frequency);
                this.frequencySchedule = frequency;

                this.currentSnapshotPersistenceTask.cancel();
                scheduleSnapshot();

            } else {
                LOGGER.warn("Please, provide a positive frequency!");
                throw new IllegalArgumentException("Please, provide a positive frequency!");
            }
        } else {
            LOGGER.warn("The current frequency is the same!");
            throw new IllegalArgumentException("The current frequency is the same!");
        }
    }

    public synchronized void scheduleSnapshot() {

        this.currentSnapshotPersistenceTask = new SnapshotPersistenceTask(marketStateLock,
                snapshotPersistence, newTicks);

        PERSIST_TIMER.scheduleAtFixedRate(currentSnapshotPersistenceTask,
                TimeUnit.SECONDS.toMillis(frequencySchedule),
                TimeUnit.SECONDS.toMillis(frequencySchedule));

        LOGGER.info("Successfully started taking snapshots in {} seconds!", frequencySchedule);

    }

}
