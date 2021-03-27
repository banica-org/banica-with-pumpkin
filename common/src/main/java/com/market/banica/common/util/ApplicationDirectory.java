package com.market.banica.common.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ApplicationDirectory {

    private static final Path config;

    private ApplicationDirectory() {
    }

    static {
        String workDir = System.getProperty("user.dir");
        config = Paths.get(workDir);
    }

    public static File getConfigFile(String fileName) throws IOException {

        File configFile = new File(config.toString() + '\\' + fileName);

        if (!configFile.exists()) {
            Files.createFile(Paths.get(configFile.getPath()));
        }

        return configFile;

    }

}
