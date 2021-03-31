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

@Component
@ManagedResource
public class PersistSchedulerImpl {

    private static final Logger LOGGER = LoggerFactory.getLogger(PersistSchedulerImpl.class);
    private int frequencySchedule;
    private SnapshotPersistenceTask snapshotPersistenceTask;
    private static final Timer PERSIST_TIMER = new Timer();
    private final SnapshotPersistence snapshotPersistence;
    private final Map<String, Set<MarketTick>> newTicks;

    @Autowired
    public PersistSchedulerImpl(SnapshotPersistence snapshotPersistence,
                                Map<String, Set<MarketTick>> newTicks) {
        this.snapshotPersistence = snapshotPersistence;
        this.newTicks = newTicks;
    }


    @ManagedOperation
    public void setFrequency(int frequency) {
        if (this.frequencySchedule != frequency) {
            if (frequency > 0) {

                LOGGER.info(String.format("Changing the snapshot frequency to %d!", frequency));
                this.frequencySchedule = frequency;

                this.snapshotPersistenceTask.cancel();
                this.snapshotPersistenceTask =
                        new SnapshotPersistenceTask(snapshotPersistence, newTicks);
                PERSIST_TIMER.scheduleAtFixedRate(snapshotPersistenceTask,
                        TimeUnit.SECONDS.toMillis(frequencySchedule),
                        TimeUnit.SECONDS.toMillis(frequencySchedule));

            } else {
                LOGGER.warn("Please, provide a positive frequency!");
            }
        } else {
            LOGGER.warn("The current frequency is the same!");
        }
    }

    public void scheduleSnapshot() {
        if (frequencySchedule != 0) {
            this.snapshotPersistenceTask = new SnapshotPersistenceTask(snapshotPersistence, newTicks);
            PERSIST_TIMER.scheduleAtFixedRate(snapshotPersistenceTask,
                    TimeUnit.SECONDS.toMillis(frequencySchedule),
                    TimeUnit.SECONDS.toMillis(frequencySchedule));
            LOGGER.info(String.format("Successfully started taking snapshots in %d seconds!",
                    frequencySchedule));
        } else {
            LOGGER.warn("There is no assigned frequency!");
        }
    }


}
