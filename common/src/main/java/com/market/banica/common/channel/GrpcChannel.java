package com.market.banica.common.channel;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.concurrent.TimeUnit;

@Component
public class GrpcChannel {
    public static final Logger log = LoggerFactory.getLogger(GrpcChannel.class);

    private final ManagedChannel managedChannel;

    public GrpcChannel(final String host, final int port) {
        log.info("Grpc channel creation with {} host and {} port is starting.", host, port);
        this.managedChannel = ManagedChannelBuilder
                .forAddress(host, port)
                .usePlaintext()
                .defaultServiceConfig(ChannelRPCConfig.getInstance().getServiceConfig())
                .enableRetry()
                .maxRetryAttempts(1000)
                .build();
        log.info("Grpc channel with {} host and {} port was build.", host, port);
    }

    @PreDestroy
    public void shutdownChannel() {
        log.info("Shutdown channel is invoked.");
        managedChannel.shutdown();

        try {
            managedChannel.awaitTermination(5L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            log.error(e.getMessage());
        }

        managedChannel.shutdownNow();

        log.info("Channel was successfully destroyed!");
    }

    public ManagedChannel getManagedChannel() {
        return this.managedChannel;
    }
}
