package com.market.banica.aurora.config;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utility {

    private static final Logger LOGGER = LoggerFactory.getLogger(Utility.class);

    public static String getObjectAsJsonString(Object dateToBeConverted, ObjectWriter objectWriter) throws JsonProcessingException {
        LOGGER.debug("In getObjectAsJsonString method");
        return objectWriter.writeValueAsString(dateToBeConverted);
    }
}
