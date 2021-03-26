package com.market.banica.aurora.service;

import com.aurora.Aurora;
import com.aurora.AuroraServiceGrpc;
import com.market.banica.aurora.manager.AuroraSubscriptionManager;
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
}
