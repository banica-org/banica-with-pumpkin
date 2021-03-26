package com.market.banica.aurora.channel;

import com.market.banica.common.channel.ChannelRPCConfig;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class MarketChannelManager {

    private final Map<String, ManagedChannel> marketChannels = new HashMap<>();

    @Value("${america.grpc.server.port}")
    int AMERICA_PORT;

    @Value("${asia.grpc.server.port}")
    int ASIA_PORT;

    @Value("${europe.grpc.server.port}")
    int EUROPE_PORT;

    public void createChannel(String channelName, String host, int port) {
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress(host, port)
                .usePlaintext()
                .enableRetry()
                .defaultServiceConfig(ChannelRPCConfig.getInstance().getServiceConfig())
                .build();
        marketChannels.put(channelName, channel);
    }

    public void shutdownChannel(String channelName) {
        ManagedChannel channel = marketChannels.get(channelName);
        if (channel == null) {
            throw new IllegalArgumentException("MarketChannel with given name does not exist!");
        }
        channel.shutdown();

        try {
            channel.awaitTermination(2L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        channel.shutdownNow();

    }

    public Map<String, ManagedChannel> getMarketChannels() {
        return Collections.unmodifiableMap(marketChannels);
    }

    public ManagedChannel getMarketChannel(String marketOrigin) {
        return this.marketChannels.get(marketOrigin);
    }

    @PostConstruct
    private void initializeMarketChannels() {
        this.createChannel("america", "localhost", AMERICA_PORT);
        this.createChannel("europe", "localhost", EUROPE_PORT);
        this.createChannel("asia", "localhost", ASIA_PORT);
    }

    @PreDestroy
    private void destroyMarketChannels() {
        this.shutdownChannel("europe");
        this.shutdownChannel("asia");
        this.shutdownChannel("america");
    }
}
