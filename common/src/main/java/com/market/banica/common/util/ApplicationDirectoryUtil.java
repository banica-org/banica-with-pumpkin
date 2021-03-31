package com.market.banica.common.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ApplicationDirectoryUtil {

    private static final String USER_DIR = "user.dir";

    private static final Path config;

    private ApplicationDirectoryUtil() {
    }

    static {

        String workDir = System.getProperty(USER_DIR);
        config = Paths.get(workDir);

    }

    public static File getConfigFile(String fileName) throws IOException {

        File configFile = new File(config.toString() + '\\' + fileName);

        if (!configFile.exists()) {
            Files.createFile(Paths.get(configFile.getPath()));
        }

        return configFile;

    }

    public static boolean doesFileExist(String fileName) {

        File configFile = new File(config.toString() + '\\' + fileName);

        return configFile.exists();

    }

}
