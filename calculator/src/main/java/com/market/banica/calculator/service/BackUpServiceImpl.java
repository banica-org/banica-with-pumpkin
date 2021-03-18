package com.market.banica.calculator.service;

import com.fasterxml.jackson.core.JsonProcessingException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.market.banica.calculator.data.contract.RecipesBase;
import com.market.banica.calculator.model.Product;
import com.market.banica.calculator.service.contract.BackUpService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class BackUpServiceImpl implements BackUpService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackUpServiceImpl.class);

    @Value("${database.backup.url}")
    private String databaseBackUpUrl;
    private final RecipesBase database;

    @Override
    @PostConstruct
    public void readBackUp() {
        LOGGER.debug("BackUp ServiceImpl: In readBackUp method");

        if (doesBackUpFileExists()) {
            try (InputStream input = new FileInputStream(databaseBackUpUrl)) {

                    ConcurrentHashMap<String, Product> data = getDataFromBackUpFile(input);

                    setDatabaseFromBackUp(data);

                LOGGER.info("Recipes database set from exterior file at location {}",databaseBackUpUrl);
            } catch (IOException e) {
                LOGGER.error("Exception thrown during reading back-up at start up", e);
            }
        }
    }

    @Override
    public void writeBackUp() {
        LOGGER.debug("BackUp ServiceImpl: In writeBackUp method");

        Map<String, Product> data = getDataFromDatabase();
        ObjectWriter objectWriter = new ObjectMapper().writerWithDefaultPrettyPrinter();

        try (OutputStream output = new FileOutputStream(databaseBackUpUrl)) {

            String jsonData = getStringFromMap(data, objectWriter);

            output.write(jsonData.getBytes(StandardCharsets.UTF_8));

            LOGGER.info("Recipes database back-up created in exterior file at location {}", databaseBackUpUrl);
        } catch (IOException e) {
            LOGGER.error("Exception thrown during writing back-up for database file: {}", database.getDatabase(), e);
        }
    }

    private boolean doesBackUpFileExists() {
        LOGGER.debug("BackUp ServiceImpl: In doesBackUpFileNotExists private method");

        return !Files.notExists(Paths.get(databaseBackUpUrl));
    }

    private ConcurrentHashMap<String, Product> getDataFromBackUpFile(InputStream input) throws IOException {
        LOGGER.debug("BackUp ServiceImpl: In getDataFromBackUpFile private method");

        return new ObjectMapper().readValue(input, new TypeReference<ConcurrentHashMap<String,Product>>(){});
    }

    private String getStringFromMap(Map<String, Product> data, ObjectWriter objectWriter) throws JsonProcessingException {
        LOGGER.debug("BackUp ServiceImpl: In getStringFromMap private method");

        return objectWriter.writeValueAsString(data);
    }

    private Map<String, Product> getDataFromDatabase() {
        LOGGER.debug("BackUp ServiceImpl: In getDataFromDatabase private method");

        return database.getDatabase();
    }

    private void setDatabaseFromBackUp(Map<String, Product> data) {
        LOGGER.debug("BackUp ServiceImpl: In setDatabaseFromBackUp private method");

        database.getDatabase().putAll(data);
    }
}
