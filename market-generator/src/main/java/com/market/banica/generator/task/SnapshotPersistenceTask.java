package com.market.banica.generator.task;

import com.market.banica.generator.model.MarketTick;
import com.market.banica.generator.util.SnapshotPersistence;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;

import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.locks.ReadWriteLock;

@EqualsAndHashCode(callSuper = false)
public class SnapshotPersistenceTask extends TimerTask {

    private final ReadWriteLock marketDataLock;
    private final SnapshotPersistence snapshotPersistence;
    private final Map<String, Set<MarketTick>> marketState;
    private final Queue<MarketTick> marketSnapshot;

    public SnapshotPersistenceTask(ReadWriteLock marketDataLock,
                                   SnapshotPersistence snapshotPersistence,
                                   Map<String, Set<MarketTick>> marketState,
                                   Queue<MarketTick> marketSnapshot) {
        this.marketDataLock = marketDataLock;
        this.snapshotPersistence = snapshotPersistence;
        this.marketState = marketState;
        this.marketSnapshot = marketSnapshot;
    }

    @SneakyThrows
    @Override
    public void run() {
        try {
            marketDataLock.readLock().lock();
            snapshotPersistence.persistMarketState(marketState, marketSnapshot);
        } finally {
            marketDataLock.readLock().unlock();
        }
    }

}
