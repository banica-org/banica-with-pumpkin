package com.market.banica.aurora.config;


import com.aurora.AuroraServiceGrpc;
import com.market.MarketServiceGrpc;
import com.orderbook.OrderBookServiceGrpc;
import io.grpc.ManagedChannel;
import org.springframework.context.annotation.Configuration;
import io.grpc.stub.AbstractStub;
import io.grpc.stub.AbstractBlockingStub;

@Configuration
public class StubManager {

    public AbstractStub<AuroraServiceGrpc.AuroraServiceStub> getAuroraStub(ManagedChannel channel) {

        return AuroraServiceGrpc.newStub(channel);
    }

    public AbstractBlockingStub<AuroraServiceGrpc.AuroraServiceBlockingStub> getAuroraBlockingStub(ManagedChannel channel) {
        return AuroraServiceGrpc.newBlockingStub(channel);
    }

    public AbstractBlockingStub<OrderBookServiceGrpc.OrderBookServiceBlockingStub> getOrderbookBlockingStub(ManagedChannel channel) {

        return OrderBookServiceGrpc.newBlockingStub(channel);
    }

    public AbstractStub<MarketServiceGrpc.MarketServiceStub> getMarketStub(ManagedChannel channel) {

        return MarketServiceGrpc.newStub(channel);
    }

    public AbstractBlockingStub<MarketServiceGrpc.MarketServiceBlockingStub> getMarketBlockingStub(ManagedChannel channel) {

        return MarketServiceGrpc.newBlockingStub(channel);
    }

}
