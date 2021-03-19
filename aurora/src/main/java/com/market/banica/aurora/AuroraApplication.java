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

        int portAmerica = 8081;
        int portAsia = 8082;
        int portEurope = 8083;
        ManagedChannel channelAsia =
                ManagedChannelBuilder.forAddress("localhost", portAsia)
                        .usePlaintext()
                        .defaultServiceConfig(ChannelRPCConfig.getInstance().getServiceConfig())
                        .enableRetry()
                        .build();
        System.out.println("-------");
        ManagedChannel channelAmerica =
                ManagedChannelBuilder.forAddress("localhost", portAmerica)
                        .usePlaintext()
                        .defaultServiceConfig(ChannelRPCConfig.getInstance().getServiceConfig())
                        .enableRetry()
                        .build();
        System.out.println("-------");
        ManagedChannel channelEurope =
                ManagedChannelBuilder.forAddress("localhost", portEurope)
                        .usePlaintext()
                        .defaultServiceConfig(ChannelRPCConfig.getInstance().getServiceConfig())
                        .enableRetry()
                        .build();


        MarketServiceGrpc.MarketServiceStub marketServiceStubAsia =
                MarketServiceGrpc.newStub(channelAsia);

        MarketServiceGrpc.MarketServiceStub marketServiceStubAmerica =
                MarketServiceGrpc.newStub(channelAmerica);

        MarketServiceGrpc.MarketServiceStub marketServiceStubEurope =
                MarketServiceGrpc.newStub(channelEurope);

        MarketDataRequest requestAsia =
                MarketDataRequest.newBuilder().setClientId("1").setGoodName("asia/eggs").build();

        MarketDataRequest requestAmerica =
                MarketDataRequest.newBuilder().setClientId("2").setGoodName("america/eggs").build();

        MarketDataRequest requestEurope =
                MarketDataRequest.newBuilder().setClientId("3").setGoodName("europe/eggs").build();


        marketServiceStubAsia.subscribeForItem(requestAsia, new StreamObserver<TickResponse>() {
            @Override
            public void onNext(TickResponse tickResponse) {
                System.out.println(tickResponse.getGoodName() + " - "
                        + tickResponse.getOrigin());
            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onCompleted() {

            }
        });
        marketServiceStubAmerica.subscribeForItem(requestAmerica, new StreamObserver<TickResponse>() {
            @Override
            public void onNext(TickResponse tickResponse) {
                System.out.println(tickResponse.getGoodName() + " - "
                        + tickResponse.getOrigin());
            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onCompleted() {

            }
        });
        marketServiceStubEurope.subscribeForItem(requestEurope, new StreamObserver<TickResponse>() {
            @Override
            public void onNext(TickResponse tickResponse) {
                System.out.println(tickResponse.getGoodName() + " - "
                        + tickResponse.getOrigin());            }

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
