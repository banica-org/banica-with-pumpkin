package com.market.banica.generator.service.task;

import com.market.banica.generator.model.MarketTick;
import com.market.banica.generator.util.SnapshotPersistence;
import lombok.SneakyThrows;

import java.util.Map;
import java.util.Set;
import java.util.TimerTask;

public class SnapshotPersistenceTask extends TimerTask {
    private final SnapshotPersistence snapshotPersistence;
    private final Map<String, Set<MarketTick>> newTicks;

    public SnapshotPersistenceTask(SnapshotPersistence snapshotPersistence,
                                   Map<String, Set<MarketTick>> newTicks) {
        this.snapshotPersistence = snapshotPersistence;
        this.newTicks = newTicks;
    }

    @SneakyThrows
    @Override
    public void run() {
        snapshotPersistence.persistTicks(newTicks);
    }
}
