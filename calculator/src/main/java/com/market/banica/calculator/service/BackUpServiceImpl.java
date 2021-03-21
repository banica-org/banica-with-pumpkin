package com.market.banica.calculator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.market.banica.calculator.data.contract.ProductBase;
import com.market.banica.calculator.model.Product;
import com.market.banica.calculator.service.contract.BackUpService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.nio.charset.StandardCharsets.UTF_8;

@Service
@RequiredArgsConstructor
public class BackUpServiceImpl implements BackUpService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackUpServiceImpl.class);

    @Value("${database.backup.url}")
    private String databaseBackUpUrl;
    private final ProductBase productBase;

    @Override
    @PostConstruct
    public void readBackUp() {
        LOGGER.debug("In readBackUp method");

        if (doesBackUpFileExists()) {

            try (InputStream input = new FileInputStream(databaseBackUpUrl)) {

                ConcurrentHashMap<String, Product> data = getDataFromBackUpFile(input);

                setDatabaseFromBackUp(data);

                LOGGER.info("Recipes database set from exterior file at location {}", databaseBackUpUrl);
            } catch (IOException e) {
                LOGGER.error("Exception thrown during reading back-up at start up", e);
            }
        }else{

            createEmptyBackUpFile();
        }
    }

    @Override
    public void writeBackUp() {
        LOGGER.debug("In writeBackUp method");

        Map<String, Product> data = getDataFromDatabase();
        ObjectWriter objectWriter = new ObjectMapper().writerWithDefaultPrettyPrinter();

        try (Writer output = new OutputStreamWriter(new FileOutputStream(databaseBackUpUrl), UTF_8)) {

            String jsonData = getStringFromMap(data, objectWriter);

            output.write(jsonData);

            LOGGER.info("Recipes database back-up created in exterior file at location {}", databaseBackUpUrl);
        } catch (IOException e) {
            LOGGER.error("Exception thrown during writing back-up for database file: {}",
                    productBase.getDatabase(), e);
        }
    }

    private void createEmptyBackUpFile() {
        try (Writer ignored = new OutputStreamWriter(
                new FileOutputStream(databaseBackUpUrl,true), UTF_8)) {

        } catch (IOException e) {
            LOGGER.error("Exception thrown during creating empty file for database back-up", e);
        }
    }

    private boolean doesBackUpFileExists() {
        LOGGER.debug("In doesBackUpFileNotExists private method");

        return new File(databaseBackUpUrl).length() != 0;
    }

    private ConcurrentHashMap<String, Product> getDataFromBackUpFile(InputStream input) throws IOException {
        LOGGER.debug("In getDataFromBackUpFile private method");

        return new ObjectMapper().readValue(input,
                new TypeReference<ConcurrentHashMap<String, Product>>() {
                });
    }

    private String getStringFromMap(Map<String, Product> data, ObjectWriter objectWriter)
            throws JsonProcessingException {
        LOGGER.debug("In getStringFromMap private method");

        return objectWriter.writeValueAsString(data);
    }

    private Map<String, Product> getDataFromDatabase() {
        LOGGER.debug("In getDataFromDatabase private method");

        return productBase.getDatabase();
    }

    private void setDatabaseFromBackUp(Map<String, Product> data) {
        LOGGER.debug("In setDatabaseFromBackUp private method");

        productBase.getDatabase().putAll(data);
    }
}
