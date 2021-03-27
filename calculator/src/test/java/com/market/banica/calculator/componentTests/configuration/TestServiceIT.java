package com.market.banica.calculator.componentTests.configuration;

import com.aurora.Aurora;
import com.aurora.AuroraServiceGrpc;
import com.orderbook.CancelSubscriptionResponse;
import com.orderbook.InterestsResponse;
import com.orderbook.ItemOrderBookResponse;
import com.orderbook.OrderBookLayer;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.IOException;

import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.mock;

@Service
@Profile(value = "testIT")
public class TestServiceIT {

    @Value(value = "${order-book-topic-prefix}")
    private String orderBookTopicPrefix;

    @Value(value = "${client-id}")
    private String clientId;

    @Value(value = "${product-name}")
    private String name;

    @Value(value = "${product-price}")
    private double price;

    private final String serverName = InProcessServerBuilder.generateName();

    public String getServerName() {

        return serverName;
    }

    public AuroraServiceGrpc.AuroraServiceBlockingStub getBlockingStub() {

        return AuroraServiceGrpc.newBlockingStub(getChannel());
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

    public AuroraServiceGrpc.AuroraServiceImplBase getGrpcServiceForCancelSubscriptionResponse() {

        return mock(AuroraServiceGrpc.AuroraServiceImplBase.class, delegatesTo(new AuroraServiceGrpc.AuroraServiceImplBase() {
            @Override
            public void request(Aurora.AuroraRequest request, StreamObserver<Aurora.AuroraResponse> responseObserver) {

                responseObserver.onNext(Aurora.AuroraResponse.newBuilder()
                        .setCancelSubscriptionResponse(getCancelSubscriptionResponse())
                        .build());

                responseObserver.onCompleted();
            }
        }));
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

    private CancelSubscriptionResponse getCancelSubscriptionResponse() {

        return CancelSubscriptionResponse.newBuilder().build();
    }
}