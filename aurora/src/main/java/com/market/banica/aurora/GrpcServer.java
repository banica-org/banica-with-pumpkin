package com.market.banica.aurora;

import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.market.banica.aurora.service.AuroraServiceImpl;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
public class GrpcServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcServer.class);

    private final ExecutorService applicationExecutor;

    private final Server server;

    @Autowired
    private GrpcServer(
            @Value("${application.executor.pool.size}") final int applicationExecutorPoolSize,
            @Value("${aurora.server.port}") final int port,
            final AuroraServiceImpl auroraService) {

        applicationExecutor = Executors.newFixedThreadPool(applicationExecutorPoolSize);

        server = NettyServerBuilder.forPort(port)
                .executor(applicationExecutor)
                .keepAliveTime(1, TimeUnit.MINUTES)
                .permitKeepAliveTime(1, TimeUnit.MINUTES)
                .addService(auroraService)
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
