package com.market.banica.generator.util;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.market.banica.common.util.ApplicationDirectoryUtil;
import com.market.banica.generator.model.MarketTick;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class SnapshotPersistence {

    private static final Logger LOGGER = LoggerFactory.getLogger(System.getenv("MARKET") + "." + SnapshotPersistence.class.getSimpleName());

    private final String stateFileName;
    private final String snapshotFileName;

    private final Kryo kryoHandle = new Kryo();

    public SnapshotPersistence(String stateFileName, String snapshotFileName) {
        initKryo();
        this.stateFileName = stateFileName;
        this.snapshotFileName = snapshotFileName;
    }

    public void persistMarketState(Map<String, Set<MarketTick>> marketState, Queue<MarketTick> marketSnapshot) throws IOException {

        Output output = new Output(new FileOutputStream(ApplicationDirectoryUtil.getConfigFile(stateFileName)));
        kryoHandle.writeClassAndObject(output, marketState);
        output.close();
        LOGGER.debug("Persisting market state!");

        marketSnapshot.clear();
        persistMarketSnapshot(marketSnapshot);

    }

    public void persistMarketSnapshot(Queue<MarketTick> marketSnapshot) throws IOException {

        Output output = new Output(new FileOutputStream(ApplicationDirectoryUtil.getConfigFile(snapshotFileName)));
        kryoHandle.writeClassAndObject(output, marketSnapshot);
        output.close();
        LOGGER.debug("Persisting market snapshot!");

    }

    @SuppressWarnings({"unchecked"})
    public Map<String, Set<MarketTick>> loadMarketState(Queue<MarketTick> marketSnapshot) throws IOException {

        Map<String, Set<MarketTick>> loadedMarketStateTicks = new ConcurrentHashMap<>();

        if (!ApplicationDirectoryUtil.doesFileExist(stateFileName)) {

            LOGGER.info("Creating \"{}\" file!", stateFileName);
            ApplicationDirectoryUtil.getConfigFile(stateFileName);

        } else if (ApplicationDirectoryUtil.getConfigFile(stateFileName).length() == 0) {

            LOGGER.info("File \"{}\" is empty, no ticks were loaded!", stateFileName);

        } else {

            Input input = new Input(new FileInputStream(ApplicationDirectoryUtil.getConfigFile(stateFileName)));
            loadedMarketStateTicks = (Map<String, Set<MarketTick>>) kryoHandle.readClassAndObject(input);
            input.close();
            LOGGER.info("Loaded market state ticks!");

        }

        for (MarketTick currentTick : marketSnapshot) {
            String good = currentTick.getGood();
            loadedMarketStateTicks.putIfAbsent(good, new TreeSet<>());
            loadedMarketStateTicks.get(good).add(currentTick);
        }

        return loadedMarketStateTicks;

    }

    @SuppressWarnings({"unchecked"})
    public Queue<MarketTick> loadMarketSnapshot() throws IOException {

        Queue<MarketTick> loadedSnapshotTicks = new LinkedBlockingQueue<>();

        if (!ApplicationDirectoryUtil.doesFileExist(snapshotFileName)) {

            LOGGER.info("Creating \"{}\" file!", snapshotFileName);
            ApplicationDirectoryUtil.getConfigFile(snapshotFileName);

        } else if (ApplicationDirectoryUtil.getConfigFile(snapshotFileName).length() == 0) {

            LOGGER.info("File \"{}\" is empty, no ticks were loaded!", snapshotFileName);

        } else {

            Input input = new Input(new FileInputStream(ApplicationDirectoryUtil.getConfigFile(snapshotFileName)));
            loadedSnapshotTicks = (Queue<MarketTick>) kryoHandle.readClassAndObject(input);
            input.close();
            LOGGER.info("Loaded snapshot database!");

        }
        return loadedSnapshotTicks;

    }

    private void initKryo() {

        kryoHandle.register(java.util.concurrent.ConcurrentHashMap.class);
        kryoHandle.register(java.util.TreeSet.class);
        kryoHandle.register(java.util.concurrent.LinkedBlockingQueue.class);
        kryoHandle.register(MarketTick.class);

    }

}