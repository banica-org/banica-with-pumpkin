package com.market.banica.aurora.config;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.market.banica.common.util.ApplicationDirectoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static java.nio.charset.StandardCharsets.UTF_8;

@Component
@EnableMBeanExport
public class GrpcClassProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcClassProvider.class);
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    String grpcClassesFilename;
    private Map<String, Class<?>> stubClassMap;
    private Map<String, String> stubNames;

    public GrpcClassProvider(@Value("${aurora.grpc.classes.filenames}") String fileName) {
        grpcClassesFilename = fileName;
        stubClassMap = new HashMap<>();
        this.loadBackup();
    }


    public Optional<Class<?>> getClass(String prefix) {
        return Optional.ofNullable(stubClassMap.get(prefix));
    }

    @ManagedOperation
    public void addClass(String key, String value) throws ClassNotFoundException {
        try {
            this.lock.writeLock().lock();
            stubNames.put(key, value);
            this.writeBackUp();

            Map.Entry<String, Class<?>> entry = this.convertStringToClass(new AbstractMap.SimpleEntry<>(key, value));
            this.stubClassMap.put(entry.getKey(), entry.getValue());
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    @ManagedOperation
    public void removeClass(String key)  {
        try {
            this.lock.writeLock().lock();
            stubNames.remove(key);
            this.writeBackUp();

            this.stubClassMap.remove(key);
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    private void loadBackup() {
        try {
            this.lock.writeLock().lock();
            stubNames = this.readFile();
            stubNames.forEach((key, value) -> {
                try {
                    addClassFromStrings(key, value);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            });
        }finally {
            this.lock.writeLock().unlock();
        }
    }
    private void addClassFromStrings(String key, String value) throws ClassNotFoundException {
        LOGGER.info("Adding new channel {} to map", key);
        Map.Entry<String, Class<?>> entry = this.convertStringToClass(new AbstractMap.SimpleEntry<>(key, value));

        this.stubClassMap.put(entry.getKey(), entry.getValue());
    }

    private Map.Entry<String, Class<?>> convertStringToClass(Map.Entry<String, String> entry) throws ClassNotFoundException {
        LOGGER.debug("converting single entry from ChannelProperty to ManagedChannel");

        return new AbstractMap.SimpleEntry<>(entry.getKey(), Class.forName(entry.getValue()));
    }

    private Map<String, String> readFile() {
        LOGGER.debug("Reading channel property from file.");

        try (InputStream input = new FileInputStream(ApplicationDirectoryUtil.getConfigFile(grpcClassesFilename))) {

            if (!ApplicationDirectoryUtil.doesFileExist(grpcClassesFilename)) {
                LOGGER.info("Creating \"{}\" file!", grpcClassesFilename);
                ApplicationDirectoryUtil.getConfigFile(grpcClassesFilename);
                return new HashMap<>();
            } else if (ApplicationDirectoryUtil.getConfigFile(grpcClassesFilename).length() == 0) {
                LOGGER.info("File \"{}\" is empty, no publishers were loaded!", grpcClassesFilename);
                return new HashMap<>();
            }
            return new ObjectMapper().readValue(input, new TypeReference<Map<String, String>>() {
            });
        } catch (IOException e) {
            LOGGER.error("Exception occurred during reading file {} with message : {}", grpcClassesFilename, e.getMessage());
        }
        return new HashMap<>();

    }

    protected void writeBackUp() {
        try {
            lock.writeLock().lock();
            LOGGER.debug("Writing back-up to json");
            ObjectWriter objectWriter = new ObjectMapper().writerWithDefaultPrettyPrinter();
            try (Writer output = new OutputStreamWriter(new FileOutputStream(ApplicationDirectoryUtil.getConfigFile(grpcClassesFilename)), UTF_8)) {
                String jsonData = Utility.getObjectAsJsonString(this.stubNames, objectWriter);
                output.write(jsonData);
                LOGGER.debug("Back-up written successfully");
            } catch (IOException e) {
                LOGGER.error("Exception thrown during writing back-up : {}", e.getMessage());
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
}
