package com.market.banica.order.book.util;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.market.banica.common.util.ApplicationDirectoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class InterestsPersistence {

    private static final Logger LOGGER = LoggerFactory.getLogger(InterestsPersistence.class);

    private final String interestsFileName;
    private final Map<String, Set<String>> interestsMap;

    private final Kryo kryoHandle = new Kryo();

    public InterestsPersistence(String interestsFileName, Map<String, Set<String>> interestsMap) {
        initKryo();
        this.interestsFileName = interestsFileName;
        this.interestsMap = interestsMap;
    }

    public void persistInterests() throws IOException {

        Output output = new Output(new FileOutputStream(ApplicationDirectoryUtil.getConfigFile(interestsFileName)));
        kryoHandle.writeClassAndObject(output, interestsMap);
        output.close();
        LOGGER.debug("Persisting orderbook interests!");

    }

    @SuppressWarnings({"unchecked"})
    public void loadInterests() throws IOException {

        if (!ApplicationDirectoryUtil.doesFileExist(interestsFileName)) {

            LOGGER.info("Creating \"{}\" file!", interestsFileName);
            ApplicationDirectoryUtil.getConfigFile(interestsFileName);

        } else if (ApplicationDirectoryUtil.getConfigFile(interestsFileName).length() == 0) {

            LOGGER.info("File \"{}\" is empty, no ticks were loaded!", interestsFileName);

        } else {

            Input input = new Input(new FileInputStream(ApplicationDirectoryUtil.getConfigFile(interestsFileName)));
            Map<String, Set<String>> loadedInterests = (Map<String, Set<String>>) kryoHandle.readClassAndObject(input);
            input.close();
            interestsMap.putAll(loadedInterests);
            LOGGER.info("Loaded orderbook interests!");

        }

    }

    private void initKryo() {

        kryoHandle.register(HashMap.class);
        kryoHandle.register(HashSet.class);

    }

}