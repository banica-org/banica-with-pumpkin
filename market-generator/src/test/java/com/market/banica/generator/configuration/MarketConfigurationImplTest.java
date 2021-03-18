package com.market.banica.generator.configuration;

import com.market.banica.generator.exception.NotFoundException;
import com.market.banica.generator.model.GoodSpecification;
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
    private static final String TEST_ORIGIN = "europe";
    private static final String BREAD_GOOD = "bread";
    private static final String MEAT_GOOD = "meat";
    private final Map<String, GoodSpecification> goods = new LinkedHashMap<>();

    @TempDir
    File file;

    private File testFile;

    private static final String PATH = "src/test/java/com/market/banica/generator/property/marketTest.properties";

    private MarketConfigurationImpl marketConfiguration;


    @Before
    public void setUp() {
        this.goods.put(String.format(ORIGIN_GOOD_PATTERN, TEST_ORIGIN, BREAD_GOOD),
                new GoodSpecification(BREAD_GOOD,
                        0, 2, 3,
                        4, 5, 6,
                        9, 8, 9));
        this.marketConfiguration = new MarketConfigurationImpl(PATH);
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
        marketConfiguration.addGoodSpecification(TEST_ORIGIN, MEAT_GOOD,
                0, 2, 3,
                -6, -2, 6,
                9, 8, 9);
        Map<String, String> propertiesFromFile = this.readFile(testFile);

        Map<String, String> europeBreadMap =
                goods.get(String.format(ORIGIN_GOOD_PATTERN, TEST_ORIGIN, BREAD_GOOD))
                        .generateProperties(TEST_ORIGIN);

        Map<String, String> europeMeatMap =
                goods.get(String.format(ORIGIN_GOOD_PATTERN, TEST_ORIGIN, MEAT_GOOD))
                        .generateProperties(TEST_ORIGIN);

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
        assertTrue(this.goods.containsKey(String.format(ORIGIN_GOOD_PATTERN, TEST_ORIGIN, BREAD_GOOD)));
        assertTrue(this.goods.containsKey(String.format(ORIGIN_GOOD_PATTERN, TEST_ORIGIN, MEAT_GOOD)));

    }

    @Test(expected = IllegalArgumentException.class)
    public void addGoodSpecificationShouldThrowIllegalArgumentExceptionWhenAddingAnExistingGoodSpecification() {
        //Arrange, Act
        marketConfiguration.addGoodSpecification(TEST_ORIGIN, BREAD_GOOD,
                0, 2, 3,
                4, 5, 6,
                9, 8, 9);
    }

    @Test
    public void removeGoodSpecificationShouldRemoveGoodSpecificationFromPropertiesFile() throws IOException {
        //Arrange, Act
        marketConfiguration.removeGoodSpecification(TEST_ORIGIN, BREAD_GOOD);
        Map<String, String> fileContent = readFile(testFile);

        //Assert
        assertEquals(0, fileContent.size());
        assertEquals(0, goods.size());
    }

    @Test(expected = NotFoundException.class)
    public void removeGoodSpecificationShouldThrowNotFoundExceptionWhenRemovingNonExistentGoodSpecification() {
        //Arrange, Act
        marketConfiguration.removeGoodSpecification(TEST_ORIGIN, MEAT_GOOD);
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
        marketConfiguration.updateGoodSpecification(TEST_ORIGIN, BREAD_GOOD,
                0, 0, 0,
                4, 5, 6,
                9, 8, 9);
        GoodSpecification updated = this.goods.get(String.format(ORIGIN_GOOD_PATTERN, TEST_ORIGIN, BREAD_GOOD));
        Map<String, String> propertiesMapFromFile = this.readFile(this.testFile);
        Map<String, String> updatedGoodSpecificationMap = updated.generateProperties(TEST_ORIGIN);

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
        marketConfiguration.updateGoodSpecification(TEST_ORIGIN, MEAT_GOOD,
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