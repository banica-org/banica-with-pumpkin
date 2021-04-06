package src.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import src.model.ChannelProperty;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class JMXConfigTest {

    private static final String CHANNEL_PREFIX = "testing-prefix";

    private static final String INVALID_CHANNEL_PREFIX = "non-existent-prefix";

    private static final String HOST = "localhost";

    private static final String PORT = "1010";

    @Mock
    private ChannelManager channels;

    private final Map<String, ChannelProperty> channelPropertyMap = new HashMap<>();

    @InjectMocks
    @Spy
    private JMXConfig jmxConfig;

    @BeforeEach
    void setUp() {
        //jmxConfig = new JMXConfig(channels);
        //   populateChannels(channelPropertyMap);
        ReflectionTestUtils.setField(jmxConfig, "channelPropertyMap", channelPropertyMap);
    }

//    @After
//    public void clearChannelsFile() {
//        jmxConfig.deleteChannel(CHANNEL_PREFIX); delete---
//    }

    @Test
    void createChannelWithInputOfAlreadySavedChannelPrefixThrowsException() {
        jmxConfig.createChannel(CHANNEL_PREFIX, HOST, PORT);
        assertThrows(IllegalArgumentException.class, () -> jmxConfig.createChannel(CHANNEL_PREFIX, HOST, PORT));
        jmxConfig.deleteChannel(CHANNEL_PREFIX);
    }

    @Test
    void createChannelWithInputOfNewChannelPrefixCreatesChannel() {
        jmxConfig.createChannel(CHANNEL_PREFIX, HOST, PORT);

        assertTrue(channelPropertyMap.containsKey(CHANNEL_PREFIX));

        ChannelProperty channelProperty = new ChannelProperty();
        channelProperty.setHost(HOST);
        channelProperty.setPort(Integer.parseInt(PORT));

        Mockito.verify(channels, Mockito.times(1)).addChannel(CHANNEL_PREFIX, channelProperty);
        jmxConfig.deleteChannel(CHANNEL_PREFIX);
    }

    @Test
    void deleteChannelWithInputOfNonExistentChannelPrefixThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> jmxConfig.deleteChannel(INVALID_CHANNEL_PREFIX));
    }

//
//    @Test
//    void editChannel() {
//    }
//
//    @Test
//    void getChannelsStatus() {
//    }
//
//    @Test
//    void writeBackUps() {
//    }


//    protected void writeBackUp() {
//
//        ObjectWriter objectWriter = new ObjectMapper().writerWithDefaultPrettyPrinter();
//
//        try (Writer output = new OutputStreamWriter(new FileOutputStream(ApplicationDirectoryUtil.getConfigFile("test.channels.json")), UTF_8)) {
//
//            String jsonData = getStringFromMap(this.channelPropertyMap, objectWriter);
//
//            output.write(jsonData);
//
//
//        } catch (IOException e) {
//            System.out.println("Exception thrown during writing back-up");
//        }
//    }
//
//    private String getStringFromMap(Map<String, ChannelProperty> data, ObjectWriter objectWriter)
//            throws JsonProcessingException {
//        System.out.println("In getStringFromMap private method");
//
//        return objectWriter.writeValueAsString(data);
//    }
//
//    private void populateChannels(Map<String, ChannelProperty> channelPropertyMap) {
//        channelPropertyMap.forEach((key, value) -> channels.addChannel(key, value));
//    }
//
//    private ConcurrentHashMap<String, ChannelProperty> readChannelsConfigsFromFile() {
//        try (InputStream input = new FileInputStream(ApplicationDirectoryUtil.getConfigFile("channels.json"))) {
//
//            return new ObjectMapper().readValue(input,
//                    new TypeReference<ConcurrentHashMap<String, ChannelProperty>>() {
//                    });
//
//        } catch (IOException e) {
//            //log exception
//        }
//        return new ConcurrentHashMap<>();
//    }
}