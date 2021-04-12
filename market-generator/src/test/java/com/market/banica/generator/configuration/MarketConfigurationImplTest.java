package com.market.banica.generator.configuration;

import com.market.banica.common.util.ApplicationDirectoryUtil;
import com.market.banica.generator.exception.NotFoundException;
import com.market.banica.generator.model.GoodSpecification;
import com.market.banica.generator.service.MarketState;
import com.market.banica.generator.service.TickGenerator;
import com.market.banica.generator.util.PersistScheduler;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketConfigurationImplTest {

    private static final ReadWriteLock propertiesLock = mock(ReadWriteLock.class);
    private static final Lock propertiesReadLock = mock(ReentrantReadWriteLock.ReadLock.class);
    private static final Lock propertiesWriteLock = mock(ReentrantReadWriteLock.WriteLock.class);

    private static final Properties properties = mock(Properties.class);
    @SuppressWarnings("unchecked")
    private static final Map<String, GoodSpecification> goods = mock(Map.class);
    private static final TickGenerator tickGenerator = mock(TickGenerator.class);
    private static final PersistScheduler persistScheduler = mock(PersistScheduler.class);

    private static final String FILE_NAME = "test-market.properties";

    private static MarketConfiguration marketConfiguration;

    private static final String GOOD_BANICA = "banica";
    private static final String GOOD_PUMPKIN = "pumpkin";
    private static final String GOOD_INVALID = "ban.ica";

    @BeforeAll
    static void beforeAll() throws IOException {

        MarketState marketState = mock(MarketState.class);
        when(marketState.getPersistScheduler()).thenReturn(persistScheduler);

        marketConfiguration = new MarketConfigurationImpl(FILE_NAME, tickGenerator, marketState);
        ReflectionTestUtils.setField(marketConfiguration, "propertiesWriteLock", propertiesLock);
        ReflectionTestUtils.setField(marketConfiguration, "properties", properties);
        ReflectionTestUtils.setField(marketConfiguration, "goods", goods);

        when(propertiesLock.readLock()).thenReturn(propertiesReadLock);
        when(propertiesLock.writeLock()).thenReturn(propertiesWriteLock);

    }

    @AfterAll
    static void afterAll() throws IOException {

        File testMarketProperties = ApplicationDirectoryUtil.getConfigFile(FILE_NAME);
        testMarketProperties.deleteOnExit();

    }

    @BeforeEach
    @SuppressWarnings("unchecked")
    void resetInvocations() {

        reset(propertiesReadLock);
        reset(propertiesWriteLock);

        reset(properties);
        reset(goods);
        reset(tickGenerator);
        reset(persistScheduler);

    }

    @Test
    void addGoodSpecification_NewGood() throws IOException {

        GoodSpecification addedGoodSpecification = new GoodSpecification(GOOD_BANICA,
                10, 30, 5,
                1.5, 3.5, 0.1,
                5, 7, 1);

        Map<String, String> generateProperties = addedGoodSpecification.generateProperties();
        when(goods.containsKey(GOOD_BANICA)).thenReturn(false);


        marketConfiguration.addGoodSpecification(GOOD_BANICA, 10, 30, 5,
                1.5, 3.5, 0.1,
                5, 7, 1);


        verify(propertiesWriteLock, times(1)).lock();
        verify(goods, times(1)).containsKey(GOOD_BANICA);
        generateProperties.forEach((key, value) -> verify(properties, times(1))
                .setProperty(key, value));
        verify(properties, times(1)).store(any(OutputStreamWriter.class), eq(null));
        verify(goods, times(1)).put(GOOD_BANICA, addedGoodSpecification);
        verify(tickGenerator, times(1)).startTickGeneration(addedGoodSpecification);
        verify(propertiesWriteLock, times(1)).unlock();

    }

    @Test
    void addGoodSpecification_OldGood() throws IOException {

        GoodSpecification addedGoodSpecification = new GoodSpecification(GOOD_BANICA,
                10, 30, 5,
                1.5, 3.5, 0.1,
                5, 7, 1);

        Map<String, String> generateProperties = addedGoodSpecification.generateProperties();
        when(goods.containsKey(GOOD_BANICA)).thenReturn(true);


        IllegalArgumentException thrownException = assertThrows(IllegalArgumentException.class,
                () -> marketConfiguration.addGoodSpecification(GOOD_BANICA,
                        10, 30, 5,
                        1.5, 3.5, 0.1,
                        5, 7, 1));


        verify(propertiesWriteLock, times(1)).lock();
        verify(goods, times(1)).containsKey(GOOD_BANICA);
        generateProperties.forEach((key, value) -> verify(properties, times(0))
                .setProperty(key, value));
        verify(properties, times(0)).store(any(OutputStreamWriter.class), eq(null));
        verify(goods, times(0)).put(GOOD_BANICA, addedGoodSpecification);
        verify(tickGenerator, times(0)).startTickGeneration(addedGoodSpecification);
        verify(propertiesWriteLock, times(1)).unlock();

        assertEquals("A good with name " + GOOD_BANICA + " already exists",
                thrownException.getMessage());

    }

    @Test
    void removeGoodSpecification_OldGood() throws IOException {

        GoodSpecification removedGoodSpecification = new GoodSpecification(GOOD_BANICA,
                10, 30, 5,
                1.5, 3.5, 0.1,
                5, 7, 1);

        Map<String, String> generateProperties = removedGoodSpecification.generateProperties();
        when(goods.containsKey(GOOD_BANICA)).thenReturn(true);
        when(goods.remove(GOOD_BANICA)).thenReturn(removedGoodSpecification);


        marketConfiguration.removeGoodSpecification(GOOD_BANICA);


        verify(propertiesWriteLock, times(1)).lock();
        verify(goods, times(1)).containsKey(GOOD_BANICA);
        verify(goods, times(1)).remove(GOOD_BANICA);
        generateProperties.forEach((key, value) -> verify(properties, times(1)).remove(key));
        verify(properties, times(1)).store(any(OutputStreamWriter.class), eq(null));
        verify(tickGenerator, times(1)).stopTickGeneration(GOOD_BANICA);
        verify(propertiesWriteLock, times(1)).unlock();

    }

    @Test
    void removeGoodSpecification_NewGood() throws IOException {

        GoodSpecification removedGoodSpecification = new GoodSpecification(GOOD_BANICA,
                10, 30, 5,
                1.5, 3.5, 0.1,
                5, 7, 1);
        Map<String, String> generateProperties = removedGoodSpecification.generateProperties();
        when(goods.containsKey(GOOD_BANICA)).thenReturn(false);


        NotFoundException thrownException = assertThrows(NotFoundException.class,
                () -> marketConfiguration.removeGoodSpecification(GOOD_BANICA));


        verify(propertiesWriteLock, times(1)).lock();
        verify(goods, times(1)).containsKey(GOOD_BANICA);
        verify(goods, times(0)).remove(GOOD_BANICA);
        generateProperties.forEach((key, value) -> verify(properties, times(0)).remove(key));
        verify(properties, times(0)).store(any(OutputStreamWriter.class), eq(null));
        verify(tickGenerator, times(0)).stopTickGeneration(GOOD_BANICA);
        verify(propertiesWriteLock, times(1)).unlock();

        assertEquals("A good with name " + GOOD_BANICA + " does not exist and it cannot be removed",
                thrownException.getMessage());

    }

    @Test
    void updateGoodSpecification_OldGood() throws IOException {

        GoodSpecification updatedGoodSpecification = new GoodSpecification(GOOD_BANICA,
                10, 30, 5,
                1.5, 3.5, 0.1,
                5, 7, 1);
        Map<String, String> generateProperties = updatedGoodSpecification.generateProperties();
        when(goods.containsKey(GOOD_BANICA)).thenReturn(true);


        marketConfiguration.updateGoodSpecification(GOOD_BANICA,
                10, 30, 5,
                1.5, 3.5, 0.1,
                5, 7, 1);


        verify(propertiesWriteLock, times(1)).lock();
        verify(goods, times(1)).containsKey(GOOD_BANICA);
        generateProperties.forEach((key, value) -> verify(properties, times(1))
                .setProperty(key, value));
        verify(properties, times(1)).store(any(OutputStreamWriter.class), eq(null));
        verify(goods, times(1)).put(GOOD_BANICA, updatedGoodSpecification);
        verify(tickGenerator, times(1)).updateTickGeneration(updatedGoodSpecification);
        verify(propertiesWriteLock, times(1)).unlock();

    }

    @Test
    void updateGoodSpecification_NewGood() throws IOException {

        GoodSpecification updatedGoodSpecification = new GoodSpecification(GOOD_BANICA,
                10, 30, 5,
                1.5, 3.5, 0.1,
                5, 7, 1);
        Map<String, String> generateProperties = updatedGoodSpecification.generateProperties();
        when(goods.containsKey(GOOD_BANICA)).thenReturn(false);


        IllegalArgumentException thrownException = assertThrows(IllegalArgumentException.class,
                () -> marketConfiguration.updateGoodSpecification(GOOD_BANICA,
                        10, 30, 5,
                        1.5, 3.5, 0.1,
                        5, 7, 1));


        verify(propertiesWriteLock, times(1)).lock();
        verify(goods, times(1)).containsKey(GOOD_BANICA);
        generateProperties.forEach((key, value) -> verify(properties, times(0))
                .setProperty(key, value));
        verify(properties, times(0)).store(any(OutputStreamWriter.class), eq(null));
        verify(goods, times(0)).put(GOOD_BANICA, updatedGoodSpecification);
        verify(tickGenerator, times(0)).updateTickGeneration(updatedGoodSpecification);
        verify(propertiesWriteLock, times(1)).unlock();

        assertEquals("A good with name " + GOOD_BANICA + " does not exist and cannot be updated",
                thrownException.getMessage());

    }

    @Test
    void setPersistenceFrequencyInSeconds() {

        int newFrequency = 55;

        marketConfiguration.setPersistenceFrequencyInSeconds(newFrequency);

        verify(persistScheduler, times(1)).setFrequency(newFrequency);

    }

    @Test
    void validateGoodSpecification_InvalidGoodName() {

        when(goods.containsKey(GOOD_INVALID)).thenReturn(false);

        IllegalArgumentException thrownException = assertThrows(IllegalArgumentException.class,
                () -> marketConfiguration.addGoodSpecification(GOOD_INVALID,
                        10, 30, 5,
                        1.5, 3.5, 0.1,
                        5, 7, 1));

        assertEquals("Good name: " + GOOD_INVALID + " cannot have dots.", thrownException.getMessage());

    }

    @Test
    void validateGoodSpecification_NegativeTickParameters() {

        when(goods.containsKey(GOOD_BANICA)).thenReturn(false);

        IllegalArgumentException thrownException = assertThrows(IllegalArgumentException.class,
                () -> marketConfiguration.addGoodSpecification(GOOD_BANICA,
                        -10, 30, 5,
                        1.5, 3.5, 0.1,
                        5, 7, 1));

        assertEquals("Low and high parameters can only be positive, steps cannot be negative.",
                thrownException.getMessage());

    }

    @Test
    void validateGoodSpecification_HighLowerThanLowTickParameters() {

        when(goods.containsKey(GOOD_BANICA)).thenReturn(false);

        IllegalArgumentException thrownException = assertThrows(IllegalArgumentException.class,
                () -> marketConfiguration.addGoodSpecification(GOOD_BANICA,
                        31, 30, 5,
                        1.5, 3.5, 0.1,
                        5, 7, 1));

        assertEquals("High parameters should be higher than low parameters.",
                thrownException.getMessage());

    }

    @Test
    void validateGoodSpecification_InvalidStepParameter() {

        when(goods.containsKey(GOOD_BANICA)).thenReturn(false);

        IllegalArgumentException thrownException = assertThrows(IllegalArgumentException.class,
                () -> marketConfiguration.addGoodSpecification(GOOD_BANICA,
                        10, 30, 21,
                        1.5, 3.5, 0.1,
                        5, 7, 1));

        assertEquals("Step parameter should be lower than the range between low and high parameters.",
                thrownException.getMessage());

    }

    @Test
    void loadProperties() throws IOException {

        GoodSpecification goodSpecification1 = new GoodSpecification(GOOD_BANICA,
                10, 30, 5,
                1.5, 3.5, 0.1,
                5, 7, 1);
        GoodSpecification goodSpecification2 = new GoodSpecification(GOOD_PUMPKIN,
                20, 40, 10,
                2.5, 5.5, 0.2,
                8, 20, 2);

        Properties propertiesForTest = new Properties();
        goodSpecification1.generateProperties().forEach(propertiesForTest::setProperty);
        goodSpecification2.generateProperties().forEach(propertiesForTest::setProperty);
        Writer output = new OutputStreamWriter(new FileOutputStream(ApplicationDirectoryUtil
                .getConfigFile(FILE_NAME)), UTF_8);
        propertiesForTest.store(output, null);

        propertiesForTest = new Properties();
        Properties propertiesForTestSpy = spy(propertiesForTest);
        Map<String, GoodSpecification> goodsForTest = new HashMap<>();
        Map<String, GoodSpecification> goodsForTestSpy = spy(goodsForTest);

        MarketState marketState = mock(MarketState.class);
        when(marketState.getPersistScheduler()).thenReturn(persistScheduler);
        MarketConfigurationImpl marketConfigurationAfterSaving = new MarketConfigurationImpl(FILE_NAME,
                tickGenerator, marketState);
        ReflectionTestUtils.setField(marketConfigurationAfterSaving, "propertiesWriteLock", propertiesLock);
        ReflectionTestUtils.setField(marketConfigurationAfterSaving, "properties", propertiesForTestSpy);
        ReflectionTestUtils.setField(marketConfigurationAfterSaving, "goods", goodsForTestSpy);
        reset(propertiesWriteLock);
        reset(tickGenerator);


        ReflectionTestUtils.invokeMethod(marketConfigurationAfterSaving,
                MarketConfigurationImpl.class, "loadProperties");


        verify(propertiesWriteLock, times(1)).lock();
        verify(propertiesForTestSpy, times(1)).load(any(FileInputStream.class));
        verify(goodsForTestSpy, times(1)).put(GOOD_BANICA, goodSpecification1);
        verify(goodsForTestSpy, times(1)).put(GOOD_PUMPKIN, goodSpecification2);
        verify(tickGenerator, times(1)).startTickGeneration(goodSpecification1);
        verify(tickGenerator, times(1)).startTickGeneration(goodSpecification2);
        verify(propertiesWriteLock, times(1)).unlock();

    }

}