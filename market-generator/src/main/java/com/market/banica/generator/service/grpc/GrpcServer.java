package com.market.banica.generator.service.grpc;

import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
public class GrpcServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(System.getenv("MARKET") + "." + GrpcServer.class.getSimpleName());

    private final ExecutorService applicationExecutor;

    private final Server server;

    @Autowired
    private GrpcServer(
            @Value("${application.executor.pool.size}") final int applicationExecutorPoolSize,
            @Value("${market.server.port}") final int port,
            final MarketService marketService) {

        applicationExecutor = Executors.newFixedThreadPool(applicationExecutorPoolSize);

        server = NettyServerBuilder.forPort(port)
                .executor(applicationExecutor)
                .keepAliveTime(1, TimeUnit.MINUTES)
                .permitKeepAliveTime(1, TimeUnit.MINUTES)
                .addService(marketService)
                .build();

    }

    @PostConstruct
    private void start() throws IOException {
        server.start();
        LOGGER.info("Server started on port {}...", server.getPort());
    }

    @PreDestroy
    private void destroy() throws InterruptedException {
        applicationExecutor.shutdownNow();
        server.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        LOGGER.info("Server stopped!");
    }

}