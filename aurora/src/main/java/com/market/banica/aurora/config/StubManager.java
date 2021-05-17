package com.market.banica.aurora.config;


import com.aurora.AuroraServiceGrpc;
import com.market.MarketServiceGrpc;
import com.orderbook.OrderBookServiceGrpc;
import io.grpc.ManagedChannel;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StubManager {

    public io.grpc.stub.AbstractStub<AuroraServiceGrpc.AuroraServiceStub> getAuroraStub(ManagedChannel channel) {

        return AuroraServiceGrpc.newStub(channel);
    }

    public io.grpc.stub.AbstractBlockingStub<AuroraServiceGrpc.AuroraServiceBlockingStub> getAuroraBlockingStub(ManagedChannel channel) {
        return AuroraServiceGrpc.newBlockingStub(channel);
    }

    public io.grpc.stub.AbstractBlockingStub<OrderBookServiceGrpc.OrderBookServiceBlockingStub> getOrderbookBlockingStub(ManagedChannel channel) {

        return OrderBookServiceGrpc.newBlockingStub(channel);
    }

    public io.grpc.stub.AbstractStub<MarketServiceGrpc.MarketServiceStub> getMarketStub(ManagedChannel channel) {

        return MarketServiceGrpc.newStub(channel);
    }

    public io.grpc.stub.AbstractBlockingStub<MarketServiceGrpc.MarketServiceBlockingStub> getMarketBlockingStub(ManagedChannel channel) {

        return MarketServiceGrpc.newBlockingStub(channel);
    }

}
