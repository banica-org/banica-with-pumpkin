package com.market.banica.generator;

import com.aurora.Aurora;
import com.google.protobuf.InvalidProtocolBufferException;
import com.market.TickResponse;
import com.orderbook.ItemOrderBookRequest;
import com.orderbook.ItemOrderBookResponse;
import com.orderbook.OrderBookServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class Runner implements CommandLineRunner {
    @Override
    public void run(String... args) throws Exception {
        ManagedChannel localhost = ManagedChannelBuilder.forAddress("localhost", 9080).usePlaintext().build();
        OrderBookServiceGrpc.OrderBookServiceBlockingStub stub = OrderBookServiceGrpc.newBlockingStub(localhost);

        Aurora.AuroraRequest request = Aurora.AuroraRequest.newBuilder().setTopic("orderbook/cheese/5").setClientId("calculator")
                .build();

        Aurora.AuroraResponse orderBookItemLayers = stub.getOrderBookItemLayers(request);
        if (orderBookItemLayers.getMessage().is(ItemOrderBookResponse.class)) {
            ItemOrderBookResponse response;

            try {
                response = orderBookItemLayers.getMessage().unpack(ItemOrderBookResponse.class);
                System.out.println(response.toString());
            } catch (InvalidProtocolBufferException e) {
                System.out.println(e.getMessage());
            }
            System.out.println();
        }
    }
}
