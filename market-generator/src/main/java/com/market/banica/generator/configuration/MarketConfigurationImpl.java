package com.market.banica.generator.configuration;

import com.market.banica.generator.exception.NotFoundException;
import com.market.banica.generator.model.GoodSpecification;
import com.market.banica.generator.property.LinkedProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.EnableMBeanExport;
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
import java.util.Map;
import java.util.Properties;
import java.util.function.BiConsumer;

@Component
@ManagedResource
@EnableMBeanExport
@ConfigurationProperties
public class MarketConfigurationImpl implements MarketConfiguration {
    private static final String ORIGIN_GOOD_PATTERN = "%s.%s";

    private final File file;

    private final Map<String, GoodSpecification> goods = new HashMap<>();

//    TODO: ADD TickGenerator when it's ready

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
        origin = origin.toLowerCase();
        good = good.toLowerCase();

        if (doesGoodExist(String.format(ORIGIN_GOOD_PATTERN, origin, good))) {
            throw new IllegalArgumentException(String.format("A good with name %s from %s already exists",
                    good.toUpperCase(), origin.toUpperCase()));
        }

        Properties properties = new LinkedProperties();

        GoodSpecification goodSpec = new GoodSpecification(good, quantityLow, quantityHigh, quantityStep,
                priceLow, priceHigh, priceStep, periodLow, periodHigh, periodStep);

        this.modifyProperties(goodSpec.generateProperties(origin), properties,
                properties::setProperty, this.file, false);
        this.goods.put(String.format(ORIGIN_GOOD_PATTERN, origin, good), goodSpec);


//        System.out.println("Adding"); TODO: replace writing in console with Logging
    }

    @Override
    @ManagedOperation
    public void removeGoodSpecification(String origin, String good) {
        origin = origin.toLowerCase();
        good = good.toLowerCase();

        if (!doesGoodExist(String.format(ORIGIN_GOOD_PATTERN, origin, good))) {
            throw new NotFoundException(String.format("A good with name %s from %s does " +
                    "not exist and it cannot be removed", good.toUpperCase(), origin.toUpperCase()));
        }
        Properties properties = new LinkedProperties();
        Map<String, String> stringStringMap = this.goods.remove(String.format(ORIGIN_GOOD_PATTERN, origin, good)).generateProperties(origin);
        this.modifyProperties(stringStringMap, properties,
                (k, v) -> properties.remove(k), this.file, false);

        this.goods.remove(String.format(ORIGIN_GOOD_PATTERN, origin, good));

//        System.out.println("Deleting"); TODO: replace writing in console with Logging
    }

    @Override
    @ManagedOperation
    public void updateGoodSpecification(String origin, String good, long quantityLow, long quantityHigh, long quantityStep,
                                        double priceLow, double priceHigh, double priceStep,
                                        int periodLow, int periodHigh, int periodStep) {
        origin = origin.toLowerCase();
        good = good.toLowerCase();

        if (!this.doesGoodExist(String.format(ORIGIN_GOOD_PATTERN, origin, good))) {
            throw new NotFoundException(String.format("A good with name %s from %s does not exist and cannot be updated",
                    good.toUpperCase(), origin.toUpperCase()));
        }

        Properties properties = new LinkedProperties();

        GoodSpecification updatedGoodSpecification = new GoodSpecification(good, quantityLow, quantityHigh, quantityStep,
                priceLow, priceHigh, priceStep,
                periodLow, periodHigh, periodStep);

        this.modifyProperties(updatedGoodSpecification.generateProperties(origin)
                , properties, properties::setProperty, this.file, false);

        this.goods.put(String.format(ORIGIN_GOOD_PATTERN, origin, good), updatedGoodSpecification);
    }

    private void modifyProperties(Map<String, String> propMap, Properties properties,
                                  BiConsumer<String, String> function, File file, boolean append) {
        if (!file.exists()) {
            try {
                Files.createFile(Paths.get(file.getPath()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try (FileInputStream inputStream = new FileInputStream(file)) {
            properties.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        propMap.forEach(function);

        try (FileOutputStream outputStream = new FileOutputStream(file, append)) {
            properties.store(outputStream, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean doesGoodExist(String good) {
        return this.goods.containsKey(good);
    }
}