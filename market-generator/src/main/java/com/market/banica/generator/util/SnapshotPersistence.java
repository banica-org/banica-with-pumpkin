package com.market.banica.generator.util;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.market.banica.common.util.ApplicationDirectoryUtil;
import com.market.banica.generator.model.MarketTick;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SnapshotPersistence {

    private static final Logger LOGGER = LoggerFactory.getLogger(SnapshotPersistence.class);

    private final String databaseFileName;

    private final Kryo kryoHandle = new Kryo();

    public SnapshotPersistence(String fileName) {
        initKryo();
        this.databaseFileName = fileName;
    }

    public void persistTicks(Map<String, Set<MarketTick>> newTicks) throws FileNotFoundException {

        Output output = new Output(new FileOutputStream(databaseFileName));
        kryoHandle.writeClassAndObject(output, newTicks);
        output.close();
        LOGGER.info("Persisting snapshot database!");

    }

    @SuppressWarnings({"unchecked"})
    public Map<String, Set<MarketTick>> loadPersistedSnapshot() throws IOException {

        Map<String, Set<MarketTick>> loadedMarketTicks = new ConcurrentHashMap<>();

        if (!ApplicationDirectoryUtil.doesFileExist(databaseFileName)) {

            LOGGER.info("Creating \"{}\" file!", databaseFileName);
            ApplicationDirectoryUtil.getConfigFile(databaseFileName);

        } else if (ApplicationDirectoryUtil.getConfigFile(databaseFileName).length() == 0) {

            LOGGER.info("File \"{}\" is empty, no ticks were loaded!", databaseFileName);

        } else {

            Input input = new Input(new FileInputStream(databaseFileName));
            loadedMarketTicks = (Map<String, Set<MarketTick>>) kryoHandle.readClassAndObject(input);
            input.close();
            LOGGER.info("Loaded snapshot database!");

        }
        return loadedMarketTicks;

    }

    private void initKryo() {

        kryoHandle.register(java.util.concurrent.ConcurrentHashMap.class);
        kryoHandle.register(java.util.TreeSet.class);
        kryoHandle.register(MarketTick.class);

    }

}