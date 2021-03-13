package com.market.banica.generator.configuration;

import com.market.banica.generator.exception.NotFoundException;
import com.market.banica.generator.model.GoodSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class MarketConfigurationImplTest {
    private static final String ORIGIN_GOOD_PATTERN = "%s.%s";
    private static final String TEST_ORIGIN = "europe";
    private static final String TEST_GOOD = "bread";
    private final Map<String, GoodSpecification> goods = new HashMap<>();

    @TempDir
    File file;

    private File testFile;


    //    @InjectMocks
    private MarketConfigurationImpl marketConfiguration;


    @BeforeEach
    public void initialize() {
        this.goods.put(String.format(ORIGIN_GOOD_PATTERN, TEST_ORIGIN, TEST_GOOD),
                new GoodSpecification(TEST_GOOD,
                        -1, 2, 3,
                        4, 5, 6,
                        9, 8, 9));
        this.marketConfiguration = new MarketConfigurationImpl("src/test/java/com/market/banica/generator/property/market.properties");
        ReflectionTestUtils.setField(marketConfiguration, "goods", goods);
        this.testFile = new File(file, "testTest.properties");
        ReflectionTestUtils.setField(marketConfiguration, "file", testFile);
    }

    @Test
    public void addGoodSpecification_Should_Work() {
        marketConfiguration.addGoodSpecification(TEST_ORIGIN, "meat",
                -1, 2, 3,
                4, 5, 6,
                9, 8, 9);

        assertEquals(2, goods.size());
        assertTrue(this.testFile.length() > 0);
        assertTrue(this.goods.containsKey(String.format(ORIGIN_GOOD_PATTERN, TEST_ORIGIN, TEST_GOOD)));
        assertTrue(this.goods.containsKey(String.format(ORIGIN_GOOD_PATTERN, TEST_ORIGIN, "meat")));

    }

    @Test
    public void addGoodSpecification_Equal_Good_Same_Origin_Should_Throw_Exception() {
        assertThrows(IllegalArgumentException.class, () -> marketConfiguration.addGoodSpecification(TEST_ORIGIN, TEST_GOOD,
                -1, 2, 3,
                4, 5, 6,
                9, 8, 9));
    }

    @Test
    public void removeGoodSpecification_Should_Work() {
        marketConfiguration.removeGoodSpecification(TEST_ORIGIN, TEST_GOOD);

        assertEquals(0, goods.size());
    }

    @Test
    public void removeGoodSpecification_Remove_Not_Existent_Good_Should_Throw_Exception() {
        assertThrows(NotFoundException.class,
                () -> marketConfiguration.removeGoodSpecification(TEST_ORIGIN, "meat"));
        assertEquals(0, this.testFile.length());
    }

    @Test
    public void updateGoodSpecification_Should_Work() {
        GoodSpecification old = new GoodSpecification(TEST_GOOD,
                -1, 2, 3,
                4, 5, 6,
                9, 8, 9);
        GoodSpecification toBeUpdated = new GoodSpecification(TEST_GOOD,
                0, 2, 3,
                4, 5, 6,
                9, 8, 9);

        marketConfiguration.updateGoodSpecification(TEST_ORIGIN, TEST_GOOD,
                0, 0, 0,
                4, 5, 6,
                9, 8, 9);
        GoodSpecification updated = this.goods.get(String.format(ORIGIN_GOOD_PATTERN, TEST_ORIGIN, TEST_GOOD));
        assertNotEquals(old, updated);
        assertNotEquals(toBeUpdated, updated);
    }

    @Test
    public void updateGoodSpecification_Update_Not_Existent_Good_Should_Throw_Exception() {
        assertThrows(NotFoundException.class,
                () -> marketConfiguration.updateGoodSpecification(TEST_ORIGIN, "meat",
                        0, 0, 0,
                        4, 5, 6,
                        9, 8, 9));
        assertEquals(0, this.testFile.length());
    }
}