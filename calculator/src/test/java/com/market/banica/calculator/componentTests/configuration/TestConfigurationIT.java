package com.market.banica.calculator.componentTests.configuration;

import com.aurora.Aurora;
import com.aurora.AuroraServiceGrpc;
import com.orderbook.ItemOrderBookResponse;
import com.orderbook.OrderBookLayer;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import lombok.AllArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.context.annotation.Profile;

import java.io.IOException;

@ConfigurationProperties(prefix = "product")
@Profile(value = "testIT")
@AllArgsConstructor
@ConstructorBinding
public class TestConfigurationIT {

    private final String name;
    private final double price;
    private final String serverName = InProcessServerBuilder.generateName();
    private final ManagedChannel channel = InProcessChannelBuilder.forName(serverName).build();

    public ManagedChannel getChannel() {

        return channel;
    }

    public AuroraServiceGrpc.AuroraServiceBlockingStub createBlockingStub() {

        return AuroraServiceGrpc.newBlockingStub(getChannel());
    }

    public Server startInProcessService() throws IOException {

        return InProcessServerBuilder.forName(serverName).directExecutor()
                .addService(getGrpcService()).build().start();
    }

    public Server startInProcessServiceWithEmptyService() throws IOException {

        return InProcessServerBuilder.forName(serverName).directExecutor()
                .addService(getEmptyGrpcService()).build().start();
    }

    private AuroraServiceGrpc.AuroraServiceImplBase getGrpcService() {

        return new AuroraServiceGrpc.AuroraServiceImplBase() {
            @Override
            public void request(Aurora.AuroraRequest request, StreamObserver<Aurora.AuroraResponse> responseObserver) {

                responseObserver.onNext(Aurora.AuroraResponse.newBuilder().
                        setItemOrderBookResponse(getItemOrderBookResponse()).
                        build());

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
}