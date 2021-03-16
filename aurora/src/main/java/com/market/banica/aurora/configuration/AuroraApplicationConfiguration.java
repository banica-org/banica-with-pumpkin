package com.market.banica.aurora.configuration;


import com.market.banica.aurora.service.AuroraService;
import com.market.banica.common.GrpcServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuroraApplicationConfiguration {

    @Bean
    public GrpcServer getGrpcServer(@Value("${grpc.server.port}") final int port, final AuroraService auroraService) {
        return new GrpcServer(port, auroraService);
    }
}
