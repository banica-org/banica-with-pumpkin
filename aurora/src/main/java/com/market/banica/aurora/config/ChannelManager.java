package com.market.banica.aurora.config;

import com.market.banica.aurora.model.ChannelProperty;
import com.market.banica.common.channel.ChannelRPCConfig;
import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@NoArgsConstructor
public class ChannelManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChannelManager.class);
    private static final int MAX_RETRY_ATTEMPTS = 720;

    private final Map<String, ManagedChannel> channels = new ConcurrentHashMap<>();


    public Optional<ManagedChannel> getChannelByKey(String key) {
        LOGGER.debug("Getting channel with key {}", key);
        Optional<ManagedChannel> managedChannel = Optional.ofNullable(channels.get(key));
        if (!managedChannel.isPresent()) {
            managedChannel = Optional.ofNullable(null);
        }
        return managedChannel;
    }

    public List<ManagedChannel> getAllChannelsContainingPrefix(String prefix) {
        String loweredPrefix = prefix.toLowerCase();
        if (prefix.equalsIgnoreCase("*")) {
            return new ArrayList<>(this.channels.values());
        }
        return this.channels.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(loweredPrefix))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    protected void addChannel(String key, ChannelProperty value) {
        LOGGER.info("Adding new channel {} to map", key);
        Map.Entry<String, ManagedChannel> entry = this.convertPropertyToChannel(new AbstractMap.SimpleEntry<>(key, value));

        entry.getValue().getState(true);

        this.channels.put(entry.getKey(), entry.getValue());
    }

    public void addChannel(String key, ManagedChannel value) {
        this.channels.put(key, value);
    }

    public ManagedChannel removeChannel(String key) {
        return this.channels.remove(key);
    }

    protected void deleteChannel(String key) {
        LOGGER.info("Deleting channel {} to map", key);
        Optional<ManagedChannel> managedChannel = Optional.ofNullable(this.channels.remove(key));

        managedChannel.ifPresent(this::shutDownChannel);

    }

    protected void editChannel(String key, ChannelProperty value) {
        LOGGER.info("Editing channel {} to map", key);
        Map.Entry<String, ManagedChannel> entry = this.convertPropertyToChannel(new AbstractMap.SimpleEntry<>(key, value));

        Optional<ManagedChannel> managedChannel = Optional.ofNullable(this.channels.remove(key));

        managedChannel.ifPresent(this::shutDownChannel);

        this.channels.put(key, entry.getValue());
    }

    protected Map<String, ManagedChannel> getChannels() {
        return this.channels;
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
                .maxRetryAttempts(MAX_RETRY_ATTEMPTS)
                .build();
    }

}
