package com.market.banica.order.book.client;

import com.aurora.Aurora;
import com.aurora.AuroraServiceGrpc;
import com.market.banica.common.ChannelRPCConfig;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Component;

@Component
public class AuroraClient {

    public void subscribe(String market, String good) {
        Aurora.AuroraRequest request = Aurora.AuroraRequest.newBuilder().setClientId("1").setTopic(market + "/" + good).build();
//        Aurora.AuroraRequest europeRequest = Aurora.AuroraRequest.newBuilder().setClientId("1").setTopic(market + "/eggs").build();
//        Aurora.AuroraRequest americaRequest = Aurora.AuroraRequest.newBuilder().setClientId("1").setTopic(market + "/eggs").build();
        ManagedChannel localhost = ManagedChannelBuilder
                .forAddress("localhost", 9091)
                .usePlaintext()
                .defaultServiceConfig(ChannelRPCConfig.getInstance().getServiceConfig())
                .build();
        AuroraServiceGrpc.newStub(localhost).subscribe(request, new StreamObserver<Aurora.AuroraResponse>() {
            @Override
            public void onNext(Aurora.AuroraResponse auroraResponse) {
                System.out.println(auroraResponse.toString());
            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onCompleted() {

            }
        });
    }
}
