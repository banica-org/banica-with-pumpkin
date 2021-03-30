package com.market.banica.generator.configuration;

import com.market.banica.common.util.ApplicationDirectoryUtil;
import com.market.banica.generator.exception.NotFoundException;
import com.market.banica.generator.model.GoodSpecification;
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

    private static final String PROPERTY_REGEX = "([a-z])+\\.([a-z])+\\.([a-z])+";

    private static final Logger LOGGER = LoggerFactory.getLogger(MarketConfigurationImpl.class);

    private final ReadWriteLock propertiesWriteLock = new ReentrantReadWriteLock();

    private final File configurationFile;
    private final Properties properties = new Properties();
    private final Map<String, GoodSpecification> goods = new HashMap<>();

    @Autowired
    public MarketConfigurationImpl(@Value("${market.properties.file.name}") String fileName) throws IOException {
        this.configurationFile = ApplicationDirectoryUtil.getConfigFile(fileName);
        loadProperties();
    }

    @Override
    @ManagedOperation
    public void addGoodSpecification(String good, long quantityLow, long quantityHigh, long quantityStep,
                                     double priceLow, double priceHigh, double priceStep,
                                     int periodLow, int periodHigh, int periodStep) {
        try {
            propertiesWriteLock.writeLock().lock();

            good = good.toLowerCase(Locale.ROOT);
            if (this.doesGoodExist(good)) {
                LOGGER.warn("A good with name {} already exists", good);
                throw new IllegalArgumentException(String.format("A good with name %s already exists", good));
            }

            GoodSpecification addedGoodSpecification = new GoodSpecification(good,
                    quantityLow, quantityHigh, quantityStep,
                    priceLow, priceHigh, priceStep,
                    periodLow, periodHigh, periodStep);

            this.modifyProperty(addedGoodSpecification);

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

            good = good.toLowerCase(Locale.ROOT);

            if (!doesGoodExist(good)) {
                LOGGER.warn("A good with name {} does not exist and it cannot be removed", good);
                throw new NotFoundException(String.format("A good with name %s does " +
                        "not exist and it cannot be removed", good));
            }

            Map<String, String> removedGoodSpecification = this.goods.remove(good).generateProperties();

            removedGoodSpecification.forEach((key, value) -> properties.remove(key));

            saveProperties();

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

            good = good.toLowerCase(Locale.ROOT);
            if (!this.doesGoodExist(good)) {
                LOGGER.warn("A good with name {} does not exist and cannot be updated", good);
                throw new IllegalArgumentException(String
                        .format("A good with name %s does not exist and cannot be updated", good));
            }

            GoodSpecification updatedGoodSpecification = new GoodSpecification(good,
                    quantityLow, quantityHigh, quantityStep,
                    priceLow, priceHigh, priceStep,
                    periodLow, periodHigh, periodStep);

            this.modifyProperty(updatedGoodSpecification);

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
            LOGGER.warn("Good name: {} cannot have dots.", goodSpecification.getName());
            throw new IllegalArgumentException("Good name: " + goodSpecification.getName() + " cannot have dots.");
        } else if (goodSpecification.getQuantityLow() <= 0 || goodSpecification.getQuantityHigh() <= 0 ||
                goodSpecification.getQuantityStep() < 0 || goodSpecification.getPriceLow() <= 0 ||
                goodSpecification.getPriceHigh() <= 0 || goodSpecification.getPriceStep() < 0 ||
                goodSpecification.getPeriodLow() <= 0 || goodSpecification.getPeriodHigh() <= 0 ||
                goodSpecification.getPeriodStep() < 0) {
            LOGGER.warn("Low and high parameters can only be positive, steps cannot be negative");
            throw new IllegalArgumentException("Low and high parameters can only be positive, steps cannot be negative");
        } else if (goodSpecification.getQuantityHigh() < goodSpecification.getQuantityLow() ||
                goodSpecification.getPriceHigh() < goodSpecification.getPriceLow() ||
                goodSpecification.getPeriodHigh() < goodSpecification.getPeriodLow()) {
            LOGGER.warn("High parameters should be higher than low parameters.");
            throw new IllegalArgumentException("High parameters should be higher than low parameters.");
        } else if (goodSpecification.getQuantityHigh() - goodSpecification.getQuantityLow() < goodSpecification.getQuantityStep() ||
                goodSpecification.getPriceHigh() - goodSpecification.getPriceLow() < goodSpecification.getPriceStep() ||
                goodSpecification.getPeriodHigh() - goodSpecification.getPeriodLow() < goodSpecification.getPeriodStep()) {
            LOGGER.warn("Step parameter should be lower than the range between low and high parameters.");
            throw new IllegalArgumentException("Step parameter should be lower than the range between low and high parameters.");
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

            List<String> distinctGoods = properties.stringPropertyNames().stream()
                    .filter(property -> property.matches(PROPERTY_REGEX))
                    .map(property -> property.split("\\.")[0])
                    .distinct()
                    .collect(Collectors.toList());

            for (String goodName : distinctGoods) {
                GoodSpecification goodSpecification = createGoodSpecification(goodName);

                goods.put(goodName, goodSpecification);
            }
        } finally {
            propertiesWriteLock.writeLock().unlock();
        }
    }

    private GoodSpecification createGoodSpecification(String goodName) {

        return new GoodSpecification(goodName,
                Long.parseLong(properties.getProperty(goodName + ".quantityrange.low")),
                Long.parseLong(properties.getProperty(goodName + ".quantityrange.high")),
                Long.parseLong(properties.getProperty(goodName + ".quantityrange.step")),
                Double.parseDouble(properties.getProperty(goodName + ".pricerange.low")),
                Double.parseDouble(properties.getProperty(goodName + ".pricerange.high")),
                Double.parseDouble(properties.getProperty(goodName + ".pricerange.step")),
                Integer.parseInt(properties.getProperty(goodName + ".tickrange.low")),
                Integer.parseInt(properties.getProperty(goodName + ".tickrange.high")),
                Integer.parseInt(properties.getProperty(goodName + ".tickrange.step")));

    }

}