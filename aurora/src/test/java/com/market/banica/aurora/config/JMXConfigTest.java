package com.market.banica.aurora.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.market.banica.aurora.model.ChannelProperty;
import com.market.banica.common.util.ApplicationDirectoryUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class JMXConfigTest {

    private static final String CHANNEL_PREFIX = "testing-prefix";

    private static final String INVALID_CHANNEL_PREFIX = "non-existent-prefix";

    private static final String HOST = "localhost";

    private static final String PORT = "1010";

    private static final String EDITED_HOST = "edited-localhost";

    private static final String TEST_FILE_NAME = "test";

    @Mock
    private ChannelManager channels;

    @Spy
    private final Map<String, ChannelProperty> channelPropertyMap = new HashMap<>();

    @InjectMocks
    @Spy
    private JMXConfig jmxConfig;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jmxConfig, "channelPropertyMap", channelPropertyMap);
        ReflectionTestUtils.setField(jmxConfig, "channelsBackupUrl", TEST_FILE_NAME);
    }

    @AfterAll
    public static void clearChannelsFile() {
        Paths.get("null").toFile().delete();
        Paths.get("test").toFile().delete();
    }

    @Test
    void createChannelWithInputOfAlreadySavedChannelPrefixThrowsException() {
        jmxConfig.createChannel(CHANNEL_PREFIX, HOST, PORT);
        assertThrows(IllegalArgumentException.class, () -> jmxConfig.createChannel(CHANNEL_PREFIX, HOST, PORT));
        jmxConfig.deleteChannel(CHANNEL_PREFIX);
    }

    @Test
    void createChannelWithInputOfNewChannelPrefixCreatesChannel() {
        //Arrange
        ChannelProperty channelProperty = new ChannelProperty();
        channelProperty.setHost(HOST);
        channelProperty.setPort(Integer.parseInt(PORT));

        //Act
        jmxConfig.createChannel(CHANNEL_PREFIX, HOST, PORT);

        //Assert
        assertTrue(channelPropertyMap.containsKey(CHANNEL_PREFIX));
        Mockito.verify(channels, Mockito.times(1)).addChannel(CHANNEL_PREFIX, channelProperty);
        Mockito.verify(jmxConfig, Mockito.times(1)).writeBackUp();
        jmxConfig.deleteChannel(CHANNEL_PREFIX);
    }

    @Test
    void deleteChannelWithInputOfNonExistentChannelPrefixThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> jmxConfig.deleteChannel(INVALID_CHANNEL_PREFIX));
    }

    @Test
    void deleteChannelWithInputOfExistingChannelPrefixDeletesChannel() {
        //Arrange, Act
        jmxConfig.createChannel(CHANNEL_PREFIX, HOST, PORT);
        assertTrue(channelPropertyMap.containsKey(CHANNEL_PREFIX));

        jmxConfig.deleteChannel(CHANNEL_PREFIX);
        assertFalse(channelPropertyMap.containsKey(CHANNEL_PREFIX));

        //Assert
        Mockito.verify(channelPropertyMap, Mockito.times(1)).remove(CHANNEL_PREFIX);
        Mockito.verify(channels, Mockito.times(1)).deleteChannel(CHANNEL_PREFIX);
        Mockito.verify(jmxConfig, Mockito.times(2)).writeBackUp();
    }


    @Test
    void editChannelWithInputOfNonExistentChannelPrefixThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> jmxConfig.editChannel(INVALID_CHANNEL_PREFIX, HOST, PORT));
    }

    @Test
    void editChannelWithInputOfExistingChannelPrefixEditsChannel() {
        //Arrange
        jmxConfig.createChannel(CHANNEL_PREFIX, HOST, PORT);
        String channelHostBeforeEdit = channelPropertyMap.get(CHANNEL_PREFIX).getHost();

        ChannelProperty channelProperty = this.channelPropertyMap.get(CHANNEL_PREFIX);
        channelProperty.setHost(EDITED_HOST);
        channelProperty.setPort(Integer.parseInt(PORT));

        //Act
        jmxConfig.editChannel(CHANNEL_PREFIX, EDITED_HOST, PORT);
        String channelHostAfterEdit = channelPropertyMap.get(CHANNEL_PREFIX).getHost();

        //Assert
        assertNotEquals(channelHostBeforeEdit, channelHostAfterEdit);
        Mockito.verify(channelPropertyMap, Mockito.times(1)).remove(CHANNEL_PREFIX);
        Mockito.verify(channelPropertyMap, Mockito.times(2)).put(CHANNEL_PREFIX, channelProperty);
        Mockito.verify(channels, Mockito.times(1)).editChannel(CHANNEL_PREFIX, channelProperty);
        Mockito.verify(jmxConfig, Mockito.times(2)).writeBackUp();
        jmxConfig.deleteChannel(CHANNEL_PREFIX);
    }

    @Test
    void getChannelsStatusVerifiesGetChannelsMethodCall() {
        jmxConfig.getChannelsStatus();
        Mockito.verify(channels, Mockito.times(1)).getChannels();
    }

    @Test
    void writeBackUp() {
        //Arrange
        ChannelProperty actualChannelProperty = new ChannelProperty();
        actualChannelProperty.setHost(HOST);
        actualChannelProperty.setPort(Integer.parseInt(PORT));

        channelPropertyMap.put("testWritingToFile", actualChannelProperty);

        //Act
        jmxConfig.writeBackUp();

        ChannelProperty expectedChannelProperty = readChannelsConfigsFromFile().get("testWritingToFile");

        //Assert
        assertEquals(expectedChannelProperty, actualChannelProperty);
    }

    private ConcurrentHashMap<String, ChannelProperty> readChannelsConfigsFromFile() {
        try (InputStream input = new FileInputStream(ApplicationDirectoryUtil.getConfigFile(TEST_FILE_NAME))) {

            return new ObjectMapper().readValue(input,
                    new TypeReference<ConcurrentHashMap<String, ChannelProperty>>() {
                    });

        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ConcurrentHashMap<>();
    }
}