package com.market.banica.generator.configuration;

import com.market.banica.generator.property.LinkedProperties;
import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

@Component
@ManagedResource
@EnableMBeanExport
public class MarketConfigurationImpl implements MarketConfiguration {
    private static final String ORIGIN_GOOD_PROPERTY_RANGE = "%s.%s.%s.%s";
    private static final String DEFAULT_FILE_PATH = "market-generator/src/main/java/com/market/banica/generator/property/market.properties";

    @Override
    @ManagedOperation
    public void addGoodSpecification(String origin, String good,
                                     long quantityLow, long quantityHigh, long quantityStep,
                                     double priceLow, double priceHigh, double priceStep,
                                     int periodLow, int periodHigh, int periodStep) {
        origin = origin.toLowerCase();
        good = good.toLowerCase();

        Properties properties = new LinkedProperties();
        this.setPropertyValues(properties, "quantityrange", origin, good, quantityLow, quantityHigh, quantityStep);
        this.setPropertyValues(properties, "pricerange", origin, good, priceLow, priceHigh, priceStep);
        this.setPropertyValues(properties, "tickrange", origin, good, periodLow, periodHigh, periodStep);

        try (FileOutputStream fileOut = new FileOutputStream(DEFAULT_FILE_PATH, true)) {
            properties.store(fileOut, null);
        } catch (Exception e) {
            System.out.println("An error occurred while adding a new food item: " + e.getMessage());
        }

//        System.out.println("Adding"); TODO: replace writing in console with Logging
    }

    @Override
    @ManagedOperation
    public void removeGoodSpecification(String origin, String name) {
        origin = origin.toLowerCase();
        name = name.toLowerCase();

        File file = new File(DEFAULT_FILE_PATH);
        Properties properties = new LinkedProperties();

        try {
            properties.load(new FileReader(file));
        } catch (IOException e) {
            System.out.printf("File with path %s cannot be opened or is not existing: %s%n", DEFAULT_FILE_PATH, e.getMessage());
        }

        this.removeGoodSpecificationFromPropertiesFile(properties, origin, name);

        try (FileOutputStream fileOut = new FileOutputStream(file, false)) {
            properties.store(fileOut, null);
        } catch (IOException e) {
            System.out.printf("An error occurred while trying to store the updated properties file (after deletion): %s%n", e.getMessage());
            e.printStackTrace();
        }

//        System.out.println("Deleting"); TODO: replace writing in console with Logging
    }

    @Override
    public void updateGoodSpecification(String origin, String good, long quantityLow, long quantityHigh, long quantityStep, double priceLow, double priceHigh, double priceStep, int periodLow, int periodHigh, int periodStep) {

    }

    private void setPropertyValues(Properties properties, String propertyName, String origin, String name, double low, double high, double step) {
        properties.setProperty(String.format(ORIGIN_GOOD_PROPERTY_RANGE, origin, name, propertyName, "low"), String.valueOf(low));
        properties.setProperty(String.format(ORIGIN_GOOD_PROPERTY_RANGE, origin, name, propertyName, "high"), String.valueOf(high));
        properties.setProperty(String.format(ORIGIN_GOOD_PROPERTY_RANGE, origin, name, propertyName, "step"), String.valueOf(step));
    }

    private void setPropertyValues(Properties properties, String propertyName, String origin, String name, long low, long high, long step) {
        properties.setProperty(String.format(ORIGIN_GOOD_PROPERTY_RANGE, origin, name, propertyName, "low"), String.valueOf(low));
        properties.setProperty(String.format(ORIGIN_GOOD_PROPERTY_RANGE, origin, name, propertyName, "high"), String.valueOf(high));
        properties.setProperty(String.format(ORIGIN_GOOD_PROPERTY_RANGE, origin, name, propertyName, "step"), String.valueOf(step));
    }

    private void removeGoodSpecificationFromPropertiesFile(Properties properties, String origin, String good) {
        this.removeProperties(properties, origin, good, "tickrange");
        this.removeProperties(properties, origin, good, "pricerange");
        this.removeProperties(properties, origin, good, "quantityrange");
    }

    private void removeProperties(Properties properties, String origin, String good, String propertyKey) {
        properties.remove(String.format(ORIGIN_GOOD_PROPERTY_RANGE, origin, good, propertyKey, "low"));
        properties.remove(String.format(ORIGIN_GOOD_PROPERTY_RANGE, origin, good, propertyKey, "high"));
        properties.remove(String.format(ORIGIN_GOOD_PROPERTY_RANGE, origin, good, propertyKey, "step"));
    }

}
