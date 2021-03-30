package src.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.market.banica.common.channel.ChannelRPCConfig;
import com.market.banica.common.util.ApplicationDirectoryUtil;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import src.model.ChannelProperty;

import javax.annotation.PreDestroy;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class ChannelManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChannelManager.class);

    private final Map<String, ManagedChannel> channels = new ConcurrentHashMap<>();


    @Autowired
    public ChannelManager() {
        this.populateChannels(this.readChannelsConfigsFromFile());
        LOGGER.debug("Map size after population {}", channels.size());
    }


    public Optional<ManagedChannel> getChannelByKey(String key) {
        LOGGER.debug("Getting channel with key {}", key);
        return Optional.ofNullable(channels.get(key));
    }

    protected void addChannel(String key, ChannelProperty value) {
        LOGGER.info("Adding new channel {} to map", key);
        Map.Entry<String, ManagedChannel> entry = this.convertPropertyToChannel(new AbstractMap.SimpleEntry<>(key, value));

        this.channels.put(entry.getKey(), entry.getValue());
    }

    protected void deleteChannel(String key) {
        LOGGER.info("Deleting channel {} to map", key);
        ManagedChannel managedChannel = this.channels.remove(key);
        this.shutDownChannel(managedChannel);
    }

    protected void editChannel(String key, ChannelProperty value) {
        LOGGER.info("Editing channel {} to map", key);
        Map.Entry<String, ManagedChannel> entry = this.convertPropertyToChannel(new AbstractMap.SimpleEntry<>(key, value));

        ManagedChannel channel = this.channels.remove(key);

        this.shutDownChannel(channel);

        this.channels.put(key, entry.getValue());
    }

    @PreDestroy
    private void shutDownAllChannels() {
        LOGGER.info("Shutting down all channels.");
        this.channels.values().forEach(this::shutDownChannel);
    }

    private void shutDownChannel(ManagedChannel channel) {
        try {
            channel.awaitTermination(2L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOGGER.error(e.getMessage());
        }
        channel.shutdownNow();
        LOGGER.debug("Channel was successfully stopped!");
    }

    private void populateChannels(ConcurrentHashMap<String, ChannelProperty> channelProperty) {
        LOGGER.debug("Populating channel map");
        channels.putAll(this.generateChannelMap(channelProperty));
    }

    private Map<String, ManagedChannel> generateChannelMap(Map<String, ChannelProperty> channelProperty) {
        LOGGER.debug("Generating map with channels from map with channelProperty");
        return channelProperty.entrySet().stream()
                .map(this::convertPropertyToChannel)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Map.Entry<String, ManagedChannel> convertPropertyToChannel(Map.Entry<String, ChannelProperty> entry) {
        LOGGER.debug("converting single entry from ChannelProperty to ManagedChannel");

        return new AbstractMap.SimpleEntry<>(entry.getKey(),
                this.buildChannel(entry.getValue().getHost(), entry.getValue().getPort()));
    }

    private ManagedChannel buildChannel(String host, int port) {
        LOGGER.debug("Building channel with host: {} and port: {}", host, port);
        return ManagedChannelBuilder
                .forAddress(host, port)
                .usePlaintext()
                .enableRetry()
                .defaultServiceConfig(ChannelRPCConfig.getInstance().getServiceConfig())
                .build();
    }

    private ConcurrentHashMap<String, ChannelProperty> readChannelsConfigsFromFile() {
        LOGGER.debug("Reading ChannelProperty from configuration file");
        try (InputStream input = new FileInputStream(ApplicationDirectoryUtil.getConfigFile("channels.json"))) {

            return new ObjectMapper().readValue(input,
                    new TypeReference<ConcurrentHashMap<String, ChannelProperty>>() {
                    });

        } catch (IOException e) {
            LOGGER.warn("An error has occurred while reading from file!");
            LOGGER.error(e.getMessage());
        }
        return new ConcurrentHashMap<>();
    }
}
