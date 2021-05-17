package com.market.banica.aurora.config;


import com.aurora.AuroraServiceGrpc;
import com.market.MarketServiceGrpc;
import com.orderbook.OrderBookServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.stub.AbstractBlockingStub;
import io.grpc.stub.AbstractStub;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StubManager {

    public io.grpc.stub.AbstractBlockingStub<? extends AbstractBlockingStub> getBlockingStub(ManagedChannel channel, String prefix) {

        if (prefix.toLowerCase().contains("aurora")) {
            return AuroraServiceGrpc.newBlockingStub(channel);
        } else if (prefix.toLowerCase().contains("orderbook")) {
            return OrderBookServiceGrpc.newBlockingStub(channel);
        } else if (prefix.toLowerCase().contains("market")) {
            return MarketServiceGrpc.newBlockingStub(channel);
        }

        throw new RuntimeException("No supported stub.");
    }

    public io.grpc.stub.AbstractStub<? extends AbstractStub> getStub(ManagedChannel channel, String prefix) {

        if (prefix.toLowerCase().contains("aurora")) {
            return AuroraServiceGrpc.newStub(channel);
        } else if (prefix.toLowerCase().contains("orderbook")) {
            return OrderBookServiceGrpc.newStub(channel);
        } else if (prefix.toLowerCase().contains("market")) {
            return MarketServiceGrpc.newStub(channel);
        }

        throw new RuntimeException("No supported stub.");
    }

}
