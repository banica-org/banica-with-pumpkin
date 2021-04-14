package com.market.banica.aurora.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.market.banica.common.util.ApplicationDirectoryUtil;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static java.nio.charset.StandardCharsets.UTF_8;
@NoArgsConstructor
@EnableMBeanExport
@ManagedResource
@Configuration
public class Publishers {
    private static final Logger LOGGER = LoggerFactory.getLogger(Publishers.class);

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private String publishersFileName;

    CopyOnWriteArrayList<String> publishersList;


    @Autowired
    public Publishers(@Value("${aurora.channels.publishers}") String fileName) {
        this.publishersFileName = fileName;
        this.publishersList = this.readPublishersFromFile();
    }

    @ManagedOperation
    public List<String> getPublishersList() {
        return publishersList;
    }

    @ManagedOperation
    public void addPublisher(String publisher) {
        if (publishersList.contains(publisher)) {
            throw new IllegalArgumentException("Publisher is already defined.");
        }
        publishersList.add(publisher);
        writeBackUp();
    }

    @ManagedOperation
    public void deletePublisher(String publisher) {
        if (!publishersList.contains(publisher)) {
            throw new IllegalArgumentException("Publisher is not defined.");
        }
        publishersList.remove(publisher);
        writeBackUp();
    }

    protected void writeBackUp() {
        try {
            lock.writeLock().lock();
            LOGGER.info("Writing back-up to json");

            ObjectWriter objectWriter = new ObjectMapper().writerWithDefaultPrettyPrinter();

            try (Writer output = new OutputStreamWriter(new FileOutputStream(ApplicationDirectoryUtil.getConfigFile(publishersFileName)), UTF_8)) {

                String jsonData = Utility.getObjectAsJsonString(publishersList, objectWriter);

                output.write(jsonData);

                LOGGER.info("Back-up written successfully");
            } catch (IOException e) {
                LOGGER.error("Exception thrown during writing back-up : {}", e.getMessage());
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    private CopyOnWriteArrayList<String> readPublishersFromFile() {
        LOGGER.debug("Reading channel property from file.");
        try (InputStream input = new FileInputStream(ApplicationDirectoryUtil.getConfigFile(publishersFileName))) {

            return new ObjectMapper().readValue(input,
                    new TypeReference<CopyOnWriteArrayList<String>>() {
                    });

        } catch (IOException e) {
            LOGGER.error("Exception occurred during reading file {} with message : {}", publishersFileName, e.getMessage());
        }
        return new CopyOnWriteArrayList<>();
    }


}
