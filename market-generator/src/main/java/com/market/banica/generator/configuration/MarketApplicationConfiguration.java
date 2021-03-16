package com.market.banica.generator.configuration;

import com.market.banica.common.GrpcServer;
import com.market.banica.generator.service.MarketServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MarketApplicationConfiguration {

    @Bean
    public GrpcServer getGrpcServer(@Value("${grpc.server.port}") final int port, final MarketServiceImpl marketService) {
        return new GrpcServer(port, marketService);
    }
}
