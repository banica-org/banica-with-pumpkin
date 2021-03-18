package com.market.banica.generator.configuration;

import com.market.banica.generator.exception.NotFoundException;
import com.market.banica.generator.model.GoodSpecification;
import com.market.banica.generator.property.LinkedProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;

@Component
@ManagedResource
public class MarketConfigurationImpl implements MarketConfiguration {
    private static final String ORIGIN_GOOD_PATTERN = "%s.%s";

    private final File file;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Logger LOGGER = LoggerFactory.getLogger(MarketConfigurationImpl.class);

    private final Map<String, GoodSpecification> goods = new HashMap<>();

    @Autowired
    public MarketConfigurationImpl(@Value("${market.properties.file.path}") String path) {
        this.file = new File(path);
    }

    @Override
    @ManagedOperation
    public void addGoodSpecification(String origin, String good,
                                     long quantityLow, long quantityHigh, long quantityStep,
                                     double priceLow, double priceHigh, double priceStep,
                                     int periodLow, int periodHigh, int periodStep) {

        try {
            this.lock.writeLock().lock();

            String errorMessage = String.format("A good with name %s from %s already exists",
                    good.toUpperCase(), origin.toUpperCase());
            String loggerMessage = "Creating and adding a new goodSpecification.";

            origin = origin.toLowerCase(Locale.getDefault());
            good = good.toLowerCase(Locale.getDefault());

            Properties properties = new LinkedProperties();
            boolean append = false;
            boolean condition = this.doesGoodExist(String.format(ORIGIN_GOOD_PATTERN,
                    origin, good));

            this.modifyProperty(origin, good,
                    new IllegalArgumentException(errorMessage),
                    condition,
                    quantityLow, quantityHigh, quantityStep,
                    priceLow, priceHigh, priceStep,
                    periodLow, periodHigh, periodStep, append, properties, properties::setProperty, loggerMessage);

        } finally {
            this.lock.writeLock().unlock();
        }
    }

    @Override
    @ManagedOperation
    public void removeGoodSpecification(String origin, String good) {
        try {
            this.lock.writeLock().lock();

            origin = origin.toLowerCase(Locale.getDefault());
            good = good.toLowerCase(Locale.getDefault());

            if (!doesGoodExist(String.format(ORIGIN_GOOD_PATTERN, origin, good))) {
                throw new NotFoundException(String.format("A good with name %s from %s does " +
                        "not exist and it cannot be removed", good.toUpperCase(), origin.toUpperCase()));
            }
            Properties properties = new LinkedProperties();
            Map<String, String> removedGoodSpecification = this.goods.remove(String.format(ORIGIN_GOOD_PATTERN, origin, good))
                    .generateProperties(origin);
            this.modifyProperties(removedGoodSpecification, properties,
                    (k, v) -> properties.remove(k), this.file, false);

            LOGGER.info("Removing an existing goodSpecification.");
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    @Override
    @ManagedOperation
    public void updateGoodSpecification(String origin, String good, long quantityLow, long quantityHigh,
                                        long quantityStep,
                                        double priceLow, double priceHigh, double priceStep,
                                        int periodLow, int periodHigh, int periodStep) {


        try {
            this.lock.writeLock().lock();

            String errorMessage =
                    String.format("A good with name %s from %s does not exist and cannot be updated", good.toUpperCase(), origin.toUpperCase());
            String loggerMessage = "Updating an existing goodSpecification.";
            origin = origin.toLowerCase(Locale.getDefault());
            good = good.toLowerCase(Locale.getDefault());

            Properties properties = new LinkedProperties();
            boolean append = false;
            boolean condition = !this.doesGoodExist(String.format(ORIGIN_GOOD_PATTERN,
                    origin, good));


            this.modifyProperty(origin, good,
                    new NotFoundException(errorMessage),
                    condition,
                    quantityLow, quantityHigh, quantityStep,
                    priceLow, priceHigh, priceStep,
                    periodLow, periodHigh, periodStep, append, properties, properties::setProperty, loggerMessage);
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    private void modifyProperty(String origin, String good,
                                RuntimeException exception, boolean condition,
                                long quantityLow, long quantityHigh, long quantityStep,
                                double priceLow, double priceHigh, double priceStep,
                                int periodLow, int periodHigh, int periodStep, boolean append,
                                Properties properties, BiConsumer<String, String> function, String loggerMessage) {

        if (condition) {
            throw exception;
        }

        GoodSpecification goodSpecification = new GoodSpecification(good,
                quantityLow, quantityHigh, quantityStep,
                priceLow, priceHigh, priceStep,
                periodLow, periodHigh, periodStep);

        validateGoodSpecification(goodSpecification);

        this.modifyProperties(goodSpecification.generateProperties(origin)
                , properties, function, this.file, append);

        this.goods.put(String.format(ORIGIN_GOOD_PATTERN, origin, good), goodSpecification);

        LOGGER.info(loggerMessage);
    }

    private void validateGoodSpecification(GoodSpecification goodSpecification) {
        for (Map.Entry<String, String> keyValue : goodSpecification.generateProperties("europe").entrySet()) {
            double currentPropertyValue = Double.parseDouble(keyValue.getValue());
            if (!(keyValue.getKey().contains("pricerange.low") ||
                    keyValue.getKey().contains("pricerange.high")) && Double.compare(currentPropertyValue, 0) < 0) {
                throw new IllegalArgumentException("Value for key: " + keyValue.getKey() + " cannot be negative");
            }
        }

    }

    private void modifyProperties(Map<String, String> propertiesMap, Properties properties,
                                  BiConsumer<String, String> function, File file, boolean append) {

        try {
            if (!file.exists()) {
                Files.createFile(Paths.get(file.getPath()));
                LOGGER.debug("Creating a new file in: {}", file.getPath());
            }
            try (FileInputStream inputStream = new FileInputStream(file)) {
                properties.load(inputStream);
            }
            propertiesMap.forEach(function);
            try (FileOutputStream outputStream = new FileOutputStream(file, append);) {
                properties.store(outputStream, null);
            }
        } catch (IOException e) {
            LOGGER.error("An error occurred while modifying the file: {}", e.getMessage());
        }
    }

    private boolean doesGoodExist(String good) {
        return this.goods.containsKey(good);
    }
}