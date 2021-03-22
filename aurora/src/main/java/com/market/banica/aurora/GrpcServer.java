package com.market.banica.aurora;

import com.market.banica.aurora.service.AuroraServiceImpl;
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
import java.util.concurrent.TimeUnit;

@Component
public class GrpcServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcServer.class);


    private final Server server;

    @Autowired
    private GrpcServer(
            @Value("${aurora.server.port}") final int port,
            final AuroraServiceImpl auroraServiceImpl) {
        server = NettyServerBuilder.forPort(port)
                .keepAliveTime(1, TimeUnit.MINUTES)
                .permitKeepAliveTime(1, TimeUnit.MINUTES)
                .addService(auroraServiceImpl)
                .build();
    }

    @PostConstruct
    private void start() throws IOException {
        server.start();
        LOGGER.info("Server started on port {}...", server.getPort());
    }

    @PreDestroy
    private void destroy() {
        server.shutdownNow();
        LOGGER.info("Server stopped!");
    }

    void awaitTermination() throws InterruptedException {
        server.awaitTermination();
    }

}
