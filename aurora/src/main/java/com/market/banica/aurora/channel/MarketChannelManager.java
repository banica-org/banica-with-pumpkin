package com.market.banica.aurora.channel;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class MarketChannelManager {

    private final Map<String, ManagedChannel> marketChannels = new HashMap<>();


    public void createChannel(String channelName, String host, int port) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        marketChannels.put(channelName, channel);
    }

    public void removeChanel(String channelName) {
        shutdownChanel(channelName);
        marketChannels.remove(channelName);
    }


    private void shutdownChanel(String channelName) {
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


}
