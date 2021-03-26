package com.market.banica.calculator.configuration;

import com.aurora.AuroraServiceGrpc;
import com.market.banica.common.channel.GrpcChannel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcConfiguration {

    @Bean
    public AuroraServiceGrpc.AuroraServiceBlockingStub getAuroraBlockingStub(@Value("${aurora.server.host}") String host
            , @Value("${aurora.server.port}") final int port) {
        return AuroraServiceGrpc.newBlockingStub(new GrpcChannel(host, port).getManagedChannel());
    }
}
