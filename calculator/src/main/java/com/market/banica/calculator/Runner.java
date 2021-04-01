package com.market.banica.calculator;

import com.aurora.Aurora;
import com.aurora.AuroraServiceGrpc;
import com.google.protobuf.InvalidProtocolBufferException;
import com.orderbook.ItemOrderBookResponse;
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

        AuroraServiceGrpc.AuroraServiceBlockingStub auroraServiceStub = AuroraServiceGrpc.newBlockingStub(localhost);

        Aurora.AuroraRequest request = Aurora.AuroraRequest.newBuilder().setTopic("orderbook/cheese/5").build();

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
