package com.market.banica.generator.configuration;

import com.market.banica.common.util.ApplicationDirectory;
import com.market.banica.generator.exception.NotFoundException;
import com.market.banica.generator.model.GoodSpecification;
import com.market.banica.generator.service.TickGenerator;
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
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

@Component
@ManagedResource
public class MarketConfigurationImpl implements MarketConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(MarketConfigurationImpl.class);

    private final ReadWriteLock propertiesWriteLock = new ReentrantReadWriteLock();

    private final File configurationFile;
    private final Properties properties = new Properties();
    private final Map<String, GoodSpecification> goods = new HashMap<>();
    private final TickGenerator tickGenerator;

    @Autowired
    public MarketConfigurationImpl(@Value("${market.properties.file.name}") String fileName,
                                   TickGenerator tickGenerator) throws IOException {
        this.configurationFile = ApplicationDirectory.getConfigFile(fileName);
        this.tickGenerator = tickGenerator;
        loadProperties();
    }

    @Override
    @ManagedOperation
    public void addGoodSpecification(String good, long quantityLow, long quantityHigh, long quantityStep,
                                     double priceLow, double priceHigh, double priceStep,
                                     int periodLow, int periodHigh, int periodStep) {
        try {
            propertiesWriteLock.writeLock().lock();

            good = good.toLowerCase(Locale.getDefault());
            if (this.doesGoodExist(good)) {
                throw new IllegalArgumentException(String.format("A good with name %s already exists",
                        good.toUpperCase()));
            }

            GoodSpecification addedGoodSpecification = new GoodSpecification(good,
                    quantityLow, quantityHigh, quantityStep,
                    priceLow, priceHigh, priceStep,
                    periodLow, periodHigh, periodStep);

            this.modifyProperty(addedGoodSpecification);

            tickGenerator.startTickGeneration(addedGoodSpecification);

            LOGGER.info("Creating and adding a new goodSpecification.");
        } finally {
            propertiesWriteLock.writeLock().unlock();
        }
    }

    @Override
    @ManagedOperation
    public void removeGoodSpecification(String good) {
        try {
            propertiesWriteLock.writeLock().lock();

            good = good.toLowerCase(Locale.getDefault());

            if (!doesGoodExist(good)) {
                throw new NotFoundException(String.format("A good with name %s does " +
                        "not exist and it cannot be removed", good.toUpperCase()));
            }

            Map<String, String> removedGoodSpecification = this.goods.remove(good).generateProperties();

            removedGoodSpecification.forEach((key, value) -> properties.remove(key));

            saveProperties();

            tickGenerator.stopTickGeneration(good);

            LOGGER.info("Removing an existing goodSpecification.");
        } finally {
            propertiesWriteLock.writeLock().unlock();
        }
    }

    @Override
    @ManagedOperation
    public void updateGoodSpecification(String good, long quantityLow, long quantityHigh, long quantityStep,
                                        double priceLow, double priceHigh, double priceStep,
                                        int periodLow, int periodHigh, int periodStep) {
        try {
            propertiesWriteLock.writeLock().lock();

            good = good.toLowerCase(Locale.getDefault());
            if (!this.doesGoodExist(good)) {
                throw new IllegalArgumentException(String
                        .format("A good with name %s does not exist and cannot be updated", good.toUpperCase()));
            }

            GoodSpecification updatedGoodSpecification = new GoodSpecification(good,
                    quantityLow, quantityHigh, quantityStep,
                    priceLow, priceHigh, priceStep,
                    periodLow, periodHigh, periodStep);

            this.modifyProperty(updatedGoodSpecification);

            tickGenerator.updateTickGeneration(updatedGoodSpecification);

            LOGGER.info("Updating an existing goodSpecification.");
        } finally {
            propertiesWriteLock.writeLock().unlock();
        }
    }

    private void modifyProperty(GoodSpecification modifiedGoodSpecification) {

        validateGoodSpecification(modifiedGoodSpecification);

        modifiedGoodSpecification.generateProperties().forEach(properties::setProperty);

        this.saveProperties();

        this.goods.put(modifiedGoodSpecification.getName(), modifiedGoodSpecification);

    }

    private void validateGoodSpecification(GoodSpecification goodSpecification) {

        if (goodSpecification.getName().contains(".")) {
            throw new IllegalArgumentException("Good name: " + goodSpecification.getName() + " cannot have dots.");
        } else if (goodSpecification.getQuantityHigh() < goodSpecification.getQuantityLow()) {
            throw new IllegalArgumentException("quantityHigh should be higher than quantityLow.");
        } else if (goodSpecification.getQuantityHigh() - goodSpecification.getQuantityLow() < goodSpecification.getQuantityStep()) {
            throw new IllegalArgumentException("quantityStep should be lower then the range between low and high.");
        } else if (goodSpecification.getPriceHigh() < goodSpecification.getPriceLow()) {
            throw new IllegalArgumentException("priceHigh should be higher than priceLow.");
        } else if (goodSpecification.getPriceHigh() - goodSpecification.getPriceLow() < goodSpecification.getPriceStep()) {
            throw new IllegalArgumentException("priceStep should be lower then the range between low and high.");
        } else if (goodSpecification.getPeriodHigh() < goodSpecification.getPeriodLow()) {
            throw new IllegalArgumentException("periodHigh should be higher than periodLow.");
        } else if (goodSpecification.getPeriodHigh() - goodSpecification.getPeriodLow() < goodSpecification.getPeriodStep()) {
            throw new IllegalArgumentException("periodStep should be lower then the range between low and high.");
        }

    }

    private void saveProperties() {

        try (Writer output = new OutputStreamWriter(new FileOutputStream(configurationFile), UTF_8)) {
            properties.store(output, null);
        } catch (IOException e) {
            LOGGER.error("An error occurred while modifying the file: {}", e.getMessage());
        }

    }

    private boolean doesGoodExist(String good) {
        return this.goods.containsKey(good);
    }

    private void loadProperties() throws IOException {
        try (FileInputStream inputStream = new FileInputStream(configurationFile)) {
            propertiesWriteLock.writeLock().lock();

            properties.load(inputStream);

            List<String> distinctProducts = properties.stringPropertyNames().stream()
                    .filter(property -> property.matches("([a-z])+\\.([a-z])+\\.([a-z])+"))
                    .map(property -> property.split("\\.")[0])
                    .distinct()
                    .collect(Collectors.toList());

            for (String product : distinctProducts) {
                GoodSpecification goodSpecification = new GoodSpecification(product,
                        Long.parseLong(properties.getProperty(product + ".quantityrange.low")),
                        Long.parseLong(properties.getProperty(product + ".quantityrange.high")),
                        Long.parseLong(properties.getProperty(product + ".quantityrange.step")),
                        Double.parseDouble(properties.getProperty(product + ".pricerange.low")),
                        Double.parseDouble(properties.getProperty(product + ".pricerange.high")),
                        Double.parseDouble(properties.getProperty(product + ".pricerange.step")),
                        Integer.parseInt(properties.getProperty(product + ".tickrange.low")),
                        Integer.parseInt(properties.getProperty(product + ".tickrange.high")),
                        Integer.parseInt(properties.getProperty(product + ".tickrange.step")));

                goods.put(product, goodSpecification);

                tickGenerator.startTickGeneration(goodSpecification);
            }
        } finally {
            propertiesWriteLock.writeLock().unlock();
        }
    }

}