package com.market.banica.aurora.service;

import com.aurora.Aurora;
import com.aurora.AuroraServiceGrpc;
import com.market.MarketDataRequest;
import com.market.MarketServiceGrpc;
import com.market.TickResponse;
import com.market.banica.aurora.client.MarketClient;
import com.market.banica.aurora.manager.AuroraSubscriptionManager;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class AuroraService extends AuroraServiceGrpc.AuroraServiceImplBase {

    @Autowired
    private AuroraSubscriptionManager subscriptionManager;


    @Override
    public void request(Aurora.AuroraRequest request, StreamObserver<Aurora.AuroraResponse> responseObserver) {

    }

    @Override
    public void subscribe(Aurora.AuroraRequest request, StreamObserver<Aurora.AuroraResponse> responseObserver) {
        subscriptionManager.subscribe(request, responseObserver);
    }

    public void test() {
        MarketDataRequest marketDataRequest = MarketDataRequest.newBuilder().setGoodName("getGood(request)").build();

        //ManagedChannel channel = marketChannelManager.getMarketChannel("test");

        ManagedChannel opa = ManagedChannelBuilder.forAddress("localhost", 8081).usePlaintext().build();

        MarketServiceGrpc.MarketServiceStub marketServiceStub = MarketServiceGrpc.newStub(opa);

        marketServiceStub.subscribeForItem(marketDataRequest, new StreamObserver<TickResponse>() {
            @Override
            public void onNext(TickResponse value) {
                System.out.println("OOOOOOOOPA");
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onCompleted() {

            }
        });

    }
}
