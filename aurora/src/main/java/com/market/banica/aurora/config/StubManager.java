package com.market.banica.aurora.config;


import com.aurora.AuroraServiceGrpc;
import com.market.MarketServiceGrpc;
import com.orderbook.OrderBookServiceGrpc;
import io.grpc.ManagedChannel;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StubManager {

    public AuroraServiceGrpc.AuroraServiceStub getAuroraStub(ManagedChannel channel){
        return AuroraServiceGrpc.newStub(channel);
    }

    public AuroraServiceGrpc.AuroraServiceBlockingStub getAuroraBlockingStub(ManagedChannel channel){
        return AuroraServiceGrpc.newBlockingStub(channel);
    }


    public OrderBookServiceGrpc.OrderBookServiceStub getOrderbookStub(ManagedChannel channel){
        return OrderBookServiceGrpc.newStub(channel);
    }

    public OrderBookServiceGrpc.OrderBookServiceBlockingStub getOrderbookBlockingStub(ManagedChannel channel){
        return OrderBookServiceGrpc.newBlockingStub(channel);
    }

    public MarketServiceGrpc.MarketServiceStub getMarketStub(ManagedChannel channel){
        return MarketServiceGrpc.newStub(channel);
    }

}
