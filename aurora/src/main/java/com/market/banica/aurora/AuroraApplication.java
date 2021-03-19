package com.market.banica.aurora;

import com.market.CatalogueRequest;
import com.market.CatalogueResponse;
import com.market.MarketDataRequest;
import com.market.MarketServiceGrpc;
import com.market.TickResponse;
import com.market.banica.common.ChannelRPCConfig;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Iterator;

@SpringBootApplication
public class AuroraApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuroraApplication.class, args);

      /*  Date date = new Date();

        long time = System.currentTimeMillis();
        System.out.println(new Date(date.getTime()));*/

//
//        int port = 8082;
        int port = 8083;
        ManagedChannel channel =
                ManagedChannelBuilder.forAddress("localhost", port)
                        .usePlaintext()
                        .defaultServiceConfig(ChannelRPCConfig.getInstance().getServiceConfig())
                        .enableRetry()
                        .build();


        MarketServiceGrpc.MarketServiceStub marketServiceStub =
                MarketServiceGrpc.newStub(channel);

        MarketDataRequest request =
                MarketDataRequest.newBuilder().setClientId("1").setGoodName("europe/eggs").build();

        marketServiceStub.subscribeForItem(request, new StreamObserver<TickResponse>() {
            @Override
            public void onNext(TickResponse tickResponse) {
                System.out.println(tickResponse.getGoodName());
            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onCompleted() {

            }
        });
        System.out.println();
    }
}
