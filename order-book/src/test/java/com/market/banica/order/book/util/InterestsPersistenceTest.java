package com.market.banica.order.book.util;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import com.market.banica.common.util.ApplicationDirectoryUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InterestsPersistenceTest {

    private static final Kryo kryoHandle = mock(Kryo.class);

    @SuppressWarnings("unchecked")
    private static final Map<String, Set<String>> interestsMap = mock(Map.class);

    private static final String interestsFileName = "test-orderbookInterests.dat";

    private static InterestsPersistence interestsPersistence;

    @BeforeAll
    static void beforeAll() {

        interestsPersistence = new InterestsPersistence(interestsFileName, interestsMap);

    }

    @SuppressWarnings("unchecked")
    @AfterEach
    void teardown() throws IOException {

        reset(kryoHandle);
        reset(interestsMap);
        File interestsFile = ApplicationDirectoryUtil.getConfigFile(interestsFileName);
        assert interestsFile.delete();

    }

    @Test
    void persistInterests() throws IOException {

        Map<String, Set<String>> interestsMap = new HashMap<>();
        interestsMap.put("client1", new HashSet<>(Arrays.asList("eggs", "flour")));
        InterestsPersistence interestsPersistence = new InterestsPersistence(interestsFileName, interestsMap);
        ReflectionTestUtils.setField(interestsPersistence, "kryoHandle", kryoHandle);

        interestsPersistence.persistInterests();

        verify(kryoHandle, times(1)).writeClassAndObject(any(Output.class), eq(interestsMap));

    }

    @Test
    void loadMarketState_WhenFileDoesNotExist() throws IOException {

        assertFalse(ApplicationDirectoryUtil.doesFileExist(interestsFileName));

        interestsPersistence.loadInterests();

        assertTrue(ApplicationDirectoryUtil.doesFileExist(interestsFileName));
        verify(interestsMap, never()).putAll(any());

    }

    @Test
    void loadMarketState_WhenFileExistsButIsEmpty() throws IOException {

        ApplicationDirectoryUtil.getConfigFile(interestsFileName);
        assertTrue(ApplicationDirectoryUtil.doesFileExist(interestsFileName));

        interestsPersistence.loadInterests();

        assertTrue(ApplicationDirectoryUtil.doesFileExist(interestsFileName));
        verify(interestsMap, never()).putAll(any());

    }

    @Test
    void loadInterests_WhenFileExistsAndHasInterests() throws IOException {

        Map<String, Set<String>> interestsMap = new HashMap<>();
        Map<String, Set<String>> expectedInterests = new HashMap<>();
        interestsPersistence = new InterestsPersistence(interestsFileName, interestsMap);

        Set<String> items1 = new HashSet<>();
        items1.add("eggs");
        items1.add("water");
        interestsMap.put("client1", items1);
        expectedInterests.put("client1", items1);

        Set<String> items2 = new HashSet<>();
        items1.add("flour");
        items1.add("cheese");
        interestsMap.put("client2", items2);
        expectedInterests.put("client2", items2);

        interestsPersistence.persistInterests();
        interestsMap.clear();


        interestsPersistence.loadInterests();

        assertEquals(expectedInterests, interestsMap);

    }

}
