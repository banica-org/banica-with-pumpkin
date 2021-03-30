package com.market.banica.generator;

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

        ItemOrderBookRequest request = ItemOrderBookRequest.newBuilder().setClientId("test").setItemName("cheese").setQuantity(8).build();

        ItemOrderBookResponse orderBookItemLayers = stub.getOrderBookItemLayers(request);
        System.out.println();
    }
}
