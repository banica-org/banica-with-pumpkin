package com.market.banica.generator.service.task;

import com.market.banica.generator.model.MarketTick;
import com.market.banica.generator.util.SnapshotPersistence;
import lombok.SneakyThrows;

import java.util.Map;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.locks.ReadWriteLock;

public class SnapshotPersistenceTask extends TimerTask {

    private final ReadWriteLock marketStateLock;
    private final SnapshotPersistence snapshotPersistence;
    private final Map<String, Set<MarketTick>> newTicks;

    public SnapshotPersistenceTask(ReadWriteLock marketStateLock,
                                   SnapshotPersistence snapshotPersistence,
                                   Map<String, Set<MarketTick>> newTicks) {
        this.marketStateLock = marketStateLock;
        this.snapshotPersistence = snapshotPersistence;
        this.newTicks = newTicks;
    }

    @SneakyThrows
    @Override
    public void run() {
        try {
            marketStateLock.readLock().lock();
            snapshotPersistence.persistTicks(newTicks);
        } finally {
            marketStateLock.readLock().unlock();
        }
    }

}
