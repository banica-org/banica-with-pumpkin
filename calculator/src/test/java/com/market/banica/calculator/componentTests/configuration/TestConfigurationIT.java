package com.market.banica.calculator.componentTests.configuration;

import com.aurora.Aurora;
import com.aurora.AuroraServiceGrpc;
import com.orderbook.InterestsResponse;
import com.orderbook.ItemOrderBookResponse;
import com.orderbook.OrderBookLayer;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.IOException;

@Configuration
@Profile(value = "testIT")
public class TestConfigurationIT {

    @Value(value = "${product-name}")
    private String name;

    @Value(value = "${product-price}")
    private double price;

    private final String serverName = InProcessServerBuilder.generateName();

    public AuroraServiceGrpc.AuroraServiceBlockingStub getBlockingStub(){

        return   AuroraServiceGrpc.newBlockingStub(getChannel());
    }

    public ManagedChannel getChannel() {

        return InProcessChannelBuilder.forName(serverName).build();
    }

    public Server startInProcessServiceForItemOrderBookResponse() throws IOException {

        return InProcessServerBuilder.forName(serverName).directExecutor()
                .addService(getGrpcServiceForItemOrderBookResponse()).build().start();
    }

    public Server startInProcessServiceForInterestResponse() throws IOException {

        return InProcessServerBuilder.forName(serverName).directExecutor()
                .addService(getGrpcServiceForInterestResponse()).build().start();
    }

    public Server startInProcessServiceWithEmptyService() throws IOException {

        return InProcessServerBuilder.forName(serverName).directExecutor()
                .addService(getEmptyGrpcService()).build().start();
    }

    private AuroraServiceGrpc.AuroraServiceImplBase getGrpcServiceForItemOrderBookResponse() {

        return new AuroraServiceGrpc.AuroraServiceImplBase() {
            @Override
            public void request(Aurora.AuroraRequest request, StreamObserver<Aurora.AuroraResponse> responseObserver) {

                responseObserver.onNext(Aurora.AuroraResponse.newBuilder()
                        .setItemOrderBookResponse(getItemOrderBookResponse()).
                                build());

                responseObserver.onCompleted();
            }
        };
    }

    private AuroraServiceGrpc.AuroraServiceImplBase getGrpcServiceForInterestResponse() {

        return new AuroraServiceGrpc.AuroraServiceImplBase() {
            @Override
            public void request(Aurora.AuroraRequest request, StreamObserver<Aurora.AuroraResponse> responseObserver) {

                responseObserver.onNext(Aurora.AuroraResponse.newBuilder()
                        .setInterestsResponse(getInterestResponse())
                        .build());

                responseObserver.onCompleted();
            }
        };
    }

    private AuroraServiceGrpc.AuroraServiceImplBase getEmptyGrpcService() {

        return new AuroraServiceGrpc.AuroraServiceImplBase() {
        };
    }

    private ItemOrderBookResponse getItemOrderBookResponse() {

        return ItemOrderBookResponse.newBuilder()
                .setItemName(name)
                .addOrderbookLayers(0, OrderBookLayer.newBuilder()
                        .setPrice(price)
                        .build())
                .build();
    }

    private InterestsResponse getInterestResponse() {

        return InterestsResponse.newBuilder().build();
    }
}