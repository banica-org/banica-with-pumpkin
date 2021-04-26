package com.market.banica.aurora.config;

import com.market.banica.aurora.model.ChannelProperty;
import io.grpc.ManagedChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@ExtendWith(MockitoExtension.class)
class ChannelManagerTest {
    private static final String CHANNEL_KEY = "market/europe";
    private static final String CHANNEL_PREFIX_MARKET = "market";
    private static final String CHANNEL_PREFIX_WEATHER = "weather";
    private static final String CHANNEL_PREFIX_ASTERISK = "*";

    private static final String HOST = "localhost";
    private static final int PORT = 1010;
    private static final int EDITED_PORT = 1011;
    private static ChannelProperty channelProperty;

    @Mock
    private ManagedChannel managedChannel;

    @Spy
    private ChannelManager channelManager;

    @BeforeEach
    void setUp() {
        channelProperty = new ChannelProperty();
        channelProperty.setHost(HOST);
        channelProperty.setPort(PORT);
    }
    
    @Test
    void getAllChannelsContainingPrefixAsteriskReturnsAllChannels() {
        //Arrange
        channelManager.getChannels().put(CHANNEL_PREFIX_MARKET, managedChannel);
        channelManager.getChannels().put(CHANNEL_PREFIX_WEATHER, managedChannel);

        List<Map.Entry<String, ManagedChannel>> expectedList = new ArrayList<>(channelManager.getChannels().entrySet());
        //Act
        List<Map.Entry<String, ManagedChannel>> actualList = channelManager.getAllChannelsContainingPrefix(CHANNEL_PREFIX_ASTERISK);

        //Assert
        assertEquals(expectedList, actualList);
    }

    @Test
    void addChannelWithValidInputCreatesAndAddsNewChannelInMap() {
        //Arrange
        assertEquals(new HashMap<>(), channelManager.getChannels());

        //Act
        channelManager.addChannel(CHANNEL_KEY, channelProperty);

        //Assert
        assertNotNull(channelManager.getChannels().get(CHANNEL_KEY));
    }

    @Test
    void deleteChannelWithInputOfExistingChannelDeletesChannelFromMap() {
        //Arrange
        assertEquals(new HashMap<>(), channelManager.getChannels());

        //Act
        channelManager.addChannel(CHANNEL_KEY, channelProperty);
        assertNotNull(channelManager.getChannels().get(CHANNEL_KEY));

        channelManager.deleteChannel(CHANNEL_KEY);
        //Assert
        assertNull(channelManager.getChannels().get(CHANNEL_KEY));
    }

    @Test
    void deleteChannelWithInputOfExistingChannelShutDownsAndDeletesChannel() {
        //Arrange
        assertEquals(new HashMap<>(), channelManager.getChannels());

        channelManager.addChannel(CHANNEL_KEY, channelProperty);
        assertNotNull(channelManager.getChannels().get(CHANNEL_KEY));

        ManagedChannel channelToBeShutDown = channelManager.getChannels().get(CHANNEL_KEY);

        //Act
        channelManager.deleteChannel(CHANNEL_KEY);

        //Assert
        assertNull(channelManager.getChannels().get(CHANNEL_KEY));
        assertTrue(channelToBeShutDown.isShutdown());
    }

    @Test
    void editChannelWithInputOfExistingChannelShutDownsAndRemovesTheExistingChannelAndCreatesNewChannelWithProvidedChannelPropertyAndKey() {
        //Arrange
        assertEquals(new HashMap<>(), channelManager.getChannels());

        channelManager.addChannel(CHANNEL_KEY, channelProperty);
        assertNotNull(channelManager.getChannels().get(CHANNEL_KEY));

        ManagedChannel channelBeforeEdit = channelManager.getChannels().get(CHANNEL_KEY);

        ChannelProperty newChannelProperties = new ChannelProperty();
        newChannelProperties.setHost(HOST);
        newChannelProperties.setPort(EDITED_PORT);

        //Act
        channelManager.editChannel(CHANNEL_KEY, newChannelProperties);
        ManagedChannel channelAfterEdit = channelManager.getChannels().get(CHANNEL_KEY);

        //Assert
        assertTrue(channelBeforeEdit.isShutdown());
        assertNotEquals(channelBeforeEdit, channelAfterEdit);
    }

    @Test
    void getChannelsReturnsChannels() {
        assertEquals(new HashMap<>(), channelManager.getChannels());

        channelManager.addChannel(CHANNEL_KEY, channelProperty);
        assertEquals(1, channelManager.getChannels().size());
    }
}