package com.market.banica.generator;

import com.aurora.Aurora;
import com.aurora.AuroraServiceGrpc;
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
        ManagedChannel localhost = ManagedChannelBuilder.forAddress("localhost", 9090)
                .usePlaintext().build();
       /* OrderBookServiceGrpc.OrderBookServiceBlockingStub stub = OrderBookServiceGrpc.newBlockingStub(localhost);

        Aurora.AuroraRequest request = Aurora.AuroraRequest.newBuilder().setTopic("orderbook/cheese/11").setClientId("calculator")
                .build();

        Aurora.AuroraResponse orderBookItemLayers = stub.getOrderBookItemLayers(request);
        if (orderBookItemLayers.getMessage().is(ItemOrderBookResponse.class)) {
            ItemOrderBookResponse response = null;

            try {
                response = orderBookItemLayers.getMessage().unpack(ItemOrderBookResponse.class);
                System.out.println(response.toString());
            } catch (InvalidProtocolBufferException e) {
                System.out.println(e.getMessage());
            }
            System.out.println();
        }*/
        AuroraServiceGrpc.AuroraServiceBlockingStub auroraServiceStub = AuroraServiceGrpc.newBlockingStub(localhost);

        Aurora.AuroraRequest request = Aurora.AuroraRequest.newBuilder().setTopic("orderbook/eggs/5").build();

        Aurora.AuroraResponse responseFromAurora = auroraServiceStub.request(request);
        if (responseFromAurora.getMessage().is(ItemOrderBookResponse.class)) {
            ItemOrderBookResponse response = null;

            try {
                response = responseFromAurora.getMessage().unpack(ItemOrderBookResponse.class);
                System.out.println(response.toString());
            } catch (InvalidProtocolBufferException e) {
                System.out.println(e.getMessage());
            }
            System.out.println();
        }
        // orderbook/...
    }
}
