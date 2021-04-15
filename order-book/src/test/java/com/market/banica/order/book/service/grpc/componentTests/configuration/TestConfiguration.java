package com.market.banica.order.book.service.grpc.componentTests.configuration;

import com.aurora.Aurora;
import com.aurora.AuroraServiceGrpc;
import com.orderbook.ItemOrderBookRequest;
import com.orderbook.ItemOrderBookResponse;
import com.orderbook.OrderBookServiceGrpc;
import io.grpc.stub.StreamObserver;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.mock;

@Configuration
@Profile(value = "testOrderBookIT")
public class TestConfiguration {


    public AuroraServiceGrpc.AuroraServiceImplBase getMockGrpcService(Aurora.AuroraResponse auroraResponse) {

        return mock(AuroraServiceGrpc.AuroraServiceImplBase.class, delegatesTo(new AuroraServiceGrpc.AuroraServiceImplBase() {
            @Override
            public void subscribe(Aurora.AuroraRequest request, StreamObserver<Aurora.AuroraResponse> responseObserver) {

                responseObserver.onNext(auroraResponse);

                responseObserver.onCompleted();
            }
        }));
    }

    public AuroraServiceGrpc.AuroraServiceImplBase aurora(Aurora.AuroraResponse auroraResponse) {

        return new AuroraServiceGrpc.AuroraServiceImplBase() {
            @Override
            public void request(Aurora.AuroraRequest request, StreamObserver<Aurora.AuroraResponse> responseObserver) {
                responseObserver.onNext(auroraResponse);

                responseObserver.onCompleted();
            }
        };
    }

    public OrderBookServiceGrpc.OrderBookServiceImplBase getGrpcOrderBookServiceItemLayers(ItemOrderBookResponse response) {

        return new OrderBookServiceGrpc.OrderBookServiceImplBase() {
            @Override
            public void getOrderBookItemLayers(ItemOrderBookRequest request, StreamObserver<ItemOrderBookResponse> responseObserver) {
                responseObserver.onNext(response);

                responseObserver.onCompleted();
            }
        };
    }

    public OrderBookServiceGrpc.OrderBookServiceImplBase getEmptyOrderBookGrpcService() {

        return new OrderBookServiceGrpc.OrderBookServiceImplBase() {
        };
    }
}