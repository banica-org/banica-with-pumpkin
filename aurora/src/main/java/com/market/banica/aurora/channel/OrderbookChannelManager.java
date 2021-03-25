package com.market.banica.aurora.channel;


import com.market.banica.common.channel.ChannelRPCConfig;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.concurrent.TimeUnit;

@Component
public class OrderbookChannelManager {

    private final ManagedChannel orderbookChannel;

    @Autowired
    public OrderbookChannelManager(@Value("${orderbook.server.port}") final int port){
         orderbookChannel = ManagedChannelBuilder
                .forAddress("localhost", port)
                .usePlaintext()
                .defaultServiceConfig(ChannelRPCConfig.getInstance().getServiceConfig())
                .build();
    }

    public ManagedChannel getChannel(){
        return orderbookChannel;
    }

    @PreDestroy
    public void shutDown(){
        try {
            orderbookChannel.awaitTermination(2L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        orderbookChannel.shutdownNow();
    }
}
