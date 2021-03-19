package com.market.banica.generator.configuration;

import com.market.banica.common.server.GrpcServer;


import com.market.banica.generator.service.MarketService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MarketApplicationConfiguration {


    @Bean
    public GrpcServer getAmericaMarketServer(@Value("${america.grpc.server.port}") final int port, final MarketService marketService) {
        return new GrpcServer(port, marketService);
    }

    @Bean
    public GrpcServer getAsiaMarketServer(@Value("${asia.grpc.server.port}") final int port, final MarketService marketService) {
        return new GrpcServer(port, marketService);
    }

    @Bean
    public GrpcServer getEuropeMarketServer(@Value("${europe.grpc.server.port}") final int port, final MarketService marketService) {
        return new GrpcServer(port, marketService);
    }
}
