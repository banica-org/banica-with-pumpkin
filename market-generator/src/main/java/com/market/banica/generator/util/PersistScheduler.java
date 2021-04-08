package com.market.banica.generator.util;

import com.market.banica.generator.model.MarketTick;
import com.market.banica.generator.task.SnapshotPersistenceTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;

public class PersistScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PersistScheduler.class);

    private final Timer persistTimer = new Timer();

    private int frequencySchedule = 60;
    private SnapshotPersistenceTask currentSnapshotPersistenceTask;

    private final ReadWriteLock marketStateLock;
    private final SnapshotPersistence snapshotPersistence;
    private final Map<String, Set<MarketTick>> newTicks;

    public PersistScheduler(ReadWriteLock marketStateLock,
                            SnapshotPersistence snapshotPersistence,
                            Map<String, Set<MarketTick>> newTicks) {
        this.marketStateLock = marketStateLock;
        this.snapshotPersistence = snapshotPersistence;
        this.newTicks = newTicks;
    }

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

        persistTimer.scheduleAtFixedRate(currentSnapshotPersistenceTask,
                TimeUnit.SECONDS.toMillis(frequencySchedule),
                TimeUnit.SECONDS.toMillis(frequencySchedule));

        LOGGER.info("Successfully started taking snapshots in {} seconds!", frequencySchedule);

    }

}
