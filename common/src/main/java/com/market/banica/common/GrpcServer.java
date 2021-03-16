package com.market.banica.common;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;


public final class GrpcServer {

    private final Server server;

    private final ExecutorService serverExecutorService;

    private final Logger LOGGER = LoggerFactory.getLogger(GrpcServer.class);

    public GrpcServer(final int port, final BindableService... services) {

        final ServerBuilder<?> serverBuilder = ServerBuilder.forPort(port);
        Stream.of(services).forEach(serverBuilder::addService);
        this.server = serverBuilder.build();

        serverExecutorService = Executors.newSingleThreadExecutor();

    }

    @PostConstruct
    public void start() throws IOException {
        this.server.start();
        LOGGER.info("starting the server on port {}", server.getPort());

        serverExecutorService.submit(() -> {
            try {
                server.awaitTermination();
            } catch (InterruptedException e) {
                LOGGER.error("An interruption occurred: {}", e.getMessage());
            }
        });

    }

    @PreDestroy
    public void stop() throws InterruptedException {
        if (this.server != null) {
            LOGGER.info("Shutting down the server");

            server.shutdown();
            server.awaitTermination(10, TimeUnit.SECONDS);
            server.shutdownNow();
        }
    }
}