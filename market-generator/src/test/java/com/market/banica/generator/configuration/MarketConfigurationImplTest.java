package com.market.banica.generator.configuration;

import com.market.banica.generator.exception.NotFoundException;
import com.market.banica.generator.model.GoodSpecification;
import com.market.banica.generator.service.MarketStateImpl;
import com.market.banica.generator.service.MarketSubscriptionManager;
import com.market.banica.generator.service.TickGeneratorImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MarketConfigurationImplTest {

    private static final String ORIGIN_GOOD_PATTERN = "%s.%s";
    private static final String BREAD_GOOD = "bread";
    private static final String MEAT_GOOD = "meat";
    private final Map<String, GoodSpecification> goods = new LinkedHashMap<>();

    @TempDir
    File file;

    private File testFile;

    private static final String FILE_NAME = "marketTest.properties";

    private MarketConfigurationImpl marketConfiguration;


    @Before
    public void setUp() throws IOException {
        this.goods.put(BREAD_GOOD,
                new GoodSpecification(BREAD_GOOD,
                        0, 2, 3,
                        4, 5, 6,
                        9, 8, 9));
        this.marketConfiguration = new MarketConfigurationImpl(FILE_NAME,new TickGeneratorImpl("europe", new MarketStateImpl(new MarketSubscriptionManager())));
        ReflectionTestUtils.setField(marketConfiguration, "goods", goods);
        this.testFile = new File(file, "marketTest.properties");
        ReflectionTestUtils.setField(marketConfiguration, "file", testFile);
    }

    @After
    public void tearDown() {
        testFile.delete();
    }

    @Test
    public void addGoodSpecificationShouldAddGoodSpecificationIntoPropertiesFile() throws IOException {
        //Arrange, Act
        marketConfiguration.addGoodSpecification(MEAT_GOOD,
                0, 2, 3,
                -6, -2, 6,
                9, 8, 9);
        Map<String, String> propertiesFromFile = this.readFile(testFile);

        Map<String, String> europeBreadMap =
                goods.get(BREAD_GOOD)
                        .generateProperties();

        Map<String, String> europeMeatMap =
                goods.get(MEAT_GOOD)
                        .generateProperties();

        Map<String, String> mergedMap = new LinkedHashMap<>();

        europeBreadMap.forEach(mergedMap::put);
        europeMeatMap.forEach(mergedMap::put);

        for (Map.Entry<String, String> keyValue : propertiesFromFile.entrySet()) {
            assertEquals(propertiesFromFile.get(keyValue.getKey()),
                    mergedMap.get(keyValue.getKey()));
        }

        //Assert
        assertEquals(2, goods.size());
        assertTrue(this.testFile.length() > 0);
        assertTrue(this.goods.containsKey(BREAD_GOOD));
        assertTrue(this.goods.containsKey(MEAT_GOOD));

    }

    @Test(expected = IllegalArgumentException.class)
    public void addGoodSpecificationShouldThrowIllegalArgumentExceptionWhenAddingAnExistingGoodSpecification() {
        //Arrange, Act
        marketConfiguration.addGoodSpecification(BREAD_GOOD,
                0, 2, 3,
                4, 5, 6,
                9, 8, 9);
    }

    @Test
    public void removeGoodSpecificationShouldRemoveGoodSpecificationFromPropertiesFile() throws IOException {
        //Arrange, Act
        marketConfiguration.removeGoodSpecification(BREAD_GOOD);
        Map<String, String> fileContent = readFile(testFile);

        //Assert
        assertEquals(0, fileContent.size());
        assertEquals(0, goods.size());
    }

    @Test(expected = NotFoundException.class)
    public void removeGoodSpecificationShouldThrowNotFoundExceptionWhenRemovingNonExistentGoodSpecification() {
        //Arrange, Act
        marketConfiguration.removeGoodSpecification(MEAT_GOOD);
    }

    @Test
    public void updateGoodSpecificationShouldWorkWhenUpdatingGoodSpecification() throws IOException {
        //Arrange
        GoodSpecification old = new GoodSpecification(BREAD_GOOD,
                0, 2, 3,
                4, 5, 6,
                9, 8, 9);
        GoodSpecification toBeUpdated = new GoodSpecification(BREAD_GOOD,
                0, 2, 3,
                4, 5, 6,
                9, 8, 9);

        //Act
        marketConfiguration.updateGoodSpecification(BREAD_GOOD,
                0, 0, 0,
                4, 5, 6,
                9, 8, 9);
        GoodSpecification updated = this.goods.get(BREAD_GOOD);
        Map<String, String> propertiesMapFromFile = this.readFile(this.testFile);
        Map<String, String> updatedGoodSpecificationMap = updated.generateProperties();

        //Assert
        assertNotEquals(old, updated);
        assertNotEquals(toBeUpdated, updated);

        for (Map.Entry<String, String> keyValue : propertiesMapFromFile.entrySet()) {
            assertEquals(propertiesMapFromFile.get(keyValue.getKey()),
                    updatedGoodSpecificationMap.get(keyValue.getKey()));
        }


    }

    @Test(expected = NotFoundException.class)
    public void updateGoodSpecificationShouldThrowNotFoundExceptionWhenUpdatingNonExistentGoodSpecification() {
        //Arrange, Act
        marketConfiguration.updateGoodSpecification(MEAT_GOOD,
                0, 0, 0,
                4, 5, 6,
                9, 8, 9);
    }

    private Map<String, String> readFile(File file) throws IOException {
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
            Map<String, String> properties = new LinkedHashMap<>();
            String line = bufferedReader.readLine();

            while (line != null) {
                if (!line.startsWith("#")) {
                    String propertyKey = line.split("=")[0];
                    String propertyValue = line.split("=")[1];
                    properties.put(propertyKey, propertyValue);
                }
                line = bufferedReader.readLine();
            }
            return properties;
        }
    }

}