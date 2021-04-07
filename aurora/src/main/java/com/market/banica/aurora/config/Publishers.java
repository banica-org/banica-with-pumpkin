package com.market.banica.aurora.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.market.banica.common.util.ApplicationDirectoryUtil;
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
import java.util.concurrent.CopyOnWriteArrayList;

import static java.nio.charset.StandardCharsets.UTF_8;

@EnableMBeanExport
@ManagedResource
@Configuration
public class Publishers {
    private static final Logger LOGGER = LoggerFactory.getLogger(Publishers.class);

    private String publishersFileName;

    CopyOnWriteArrayList<String> publishers;

    @Autowired
    public Publishers(@Value("${aurora.channels.publishers}") String fileName) {
        this.publishersFileName = fileName;
        this.publishers = this.readPublishersFromFile();
    }

    @ManagedOperation
    public CopyOnWriteArrayList<String> getPublishers() {
        return publishers;
    }

    @ManagedOperation
    public void addPublisher(String publisher) {
        if (publishers.contains(publisher)) {
            throw new IllegalArgumentException("Publisher is already defined.");
        }
        publishers.add(publisher);
        writeBackUp();
    }

    @ManagedOperation
    public void deletePublisher(String publisher){
        if (!publishers.contains(publisher)){
            throw new IllegalArgumentException("Publisher is not defined.");
        }
        publishers.remove(publisher);
        writeBackUp();
    }

    protected void writeBackUp() {
        LOGGER.info("Writing back-up to json");

        ObjectWriter objectWriter = new ObjectMapper().writerWithDefaultPrettyPrinter();

        try (Writer output = new OutputStreamWriter(new FileOutputStream(ApplicationDirectoryUtil.getConfigFile(publishersFileName)), UTF_8)) {

            String jsonData = getStringFromList(publishers, objectWriter);

            output.write(jsonData);

            LOGGER.info("Back-up written successfully");
        } catch (IOException e) {
            LOGGER.error("Exception thrown during writing back-up");
        }
    }

    private CopyOnWriteArrayList<String> readPublishersFromFile() {
        LOGGER.debug("Reading channel property from file.");
        try (InputStream input = new FileInputStream(ApplicationDirectoryUtil.getConfigFile(publishersFileName))) {

            return new ObjectMapper().readValue(input,
                    new TypeReference<CopyOnWriteArrayList<String>>() {
                    });

        } catch (IOException e) {
            //log exception
        }
        return new CopyOnWriteArrayList<>();
    }


    private String getStringFromList(CopyOnWriteArrayList<String> data, ObjectWriter objectWriter)
            throws JsonProcessingException {
        LOGGER.debug("In getStringFromMap private method");

        return objectWriter.writeValueAsString(data);
    }
}
