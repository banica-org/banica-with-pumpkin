package com.market.banica.aurora.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.market.banica.common.util.ApplicationDirectoryUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class PublishersTest {

    private static final String TEST_FILE_NAME = "publishers-test";

    private static final String PUBLISHER = "test-publisher";

    @Spy
    private CopyOnWriteArrayList<String> publishersList;

    @Spy
    private Publishers publishers;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(publishers, "publishersList", publishersList);
        ReflectionTestUtils.setField(publishers, "publishersFileName", TEST_FILE_NAME);
    }

    @AfterAll
    public static void clearChannelsFile() {
        Paths.get("publishers-test").toFile().delete();
    }

    @Test
    void getPublishersListReturnsPublisherList() {
        publishersList.add(PUBLISHER);
        assertEquals(publishersList, publishers.getPublishersList());
    }

    @Test
    void addPublisherWithInputOfExistentPublisherThrowsException() {
        publishers.addPublisher(PUBLISHER);
        assertThrows(IllegalArgumentException.class, () -> publishers.addPublisher(PUBLISHER));
    }

    @Test
    void addPublisherWithInputOfNonExistentPublisherAddsNewPublisher() {
        publishers.addPublisher(PUBLISHER);
        assertTrue(publishersList.contains(PUBLISHER));
    }

    @Test
    void addPublisherVerifiesWriteBackUpCall() {
        publishers.addPublisher(PUBLISHER);
        Mockito.verify(publishers, Mockito.times(1)).writeBackUp();
    }

    @Test
    void deletePublisherWithInputOfNonExistentPublisherThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> publishers.deletePublisher(PUBLISHER));
    }

    @Test
    void deletePublisherWithInputOfExistentPublisherRemovesPublisher() {
        publishers.addPublisher(PUBLISHER);
        publishers.deletePublisher(PUBLISHER);
        assertFalse(publishersList.contains(PUBLISHER));
    }

    @Test
    void deletePublisherVerifiesWriteBackUpCall() {
        publishers.addPublisher(PUBLISHER);
        publishers.deletePublisher(PUBLISHER);
        Mockito.verify(publishers, Mockito.times(2)).writeBackUp();
    }

    @Test
    void writeBackUpSavesPublisherInBackUpFile() {
        //Arrange, Act
        publishers.addPublisher(PUBLISHER);

        //Assert
        assertEquals(PUBLISHER, readPublishersFromFile().get(0));
    }

    private CopyOnWriteArrayList<String> readPublishersFromFile() {

        try (InputStream input = new FileInputStream(ApplicationDirectoryUtil.getConfigFile(TEST_FILE_NAME))) {

            return new ObjectMapper().readValue(input,
                    new TypeReference<CopyOnWriteArrayList<String>>() {
                    });

        } catch (IOException e) {
            System.out.println("Exception message");
        }
        return new CopyOnWriteArrayList<>();
    }
}