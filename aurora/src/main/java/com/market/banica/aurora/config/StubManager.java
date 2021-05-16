package com.market.banica.aurora.config;


import com.aurora.AuroraServiceGrpc;
import com.market.MarketServiceGrpc;
import com.orderbook.OrderBookServiceGrpc;
import io.grpc.ManagedChannel;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Method;
import java.util.AbstractMap;
import java.util.Map;

@Configuration
public class StubManager {

    public Map.Entry<AuroraServiceGrpc.AuroraServiceStub, Method[]> getAuroraStub(ManagedChannel channel) {

        AuroraServiceGrpc.AuroraServiceStub auroraServiceStub = AuroraServiceGrpc.newStub(channel);
        Class auroraStub = auroraServiceStub.getClass();
        return new AbstractMap.SimpleEntry<>(auroraServiceStub, auroraStub.getDeclaredMethods());

    }

    public Map.Entry<OrderBookServiceGrpc.OrderBookServiceBlockingStub, Method[]> getOrderbookBlockingStub(ManagedChannel channel) {

        OrderBookServiceGrpc.OrderBookServiceBlockingStub orderBookServiceBlockingStub = OrderBookServiceGrpc.newBlockingStub(channel);
        Class orderBookStub = orderBookServiceBlockingStub.getClass();
        return new AbstractMap.SimpleEntry<>(orderBookServiceBlockingStub, orderBookStub.getDeclaredMethods());

    }

    public Map.Entry<MarketServiceGrpc.MarketServiceStub, Method[]> getMarketStub(ManagedChannel channel) {

        MarketServiceGrpc.MarketServiceStub marketServiceStub = MarketServiceGrpc.newStub(channel);
        Class marketStub = marketServiceStub.getClass();
        return new AbstractMap.SimpleEntry<>(marketServiceStub, marketStub.getDeclaredMethods());
    }

}
