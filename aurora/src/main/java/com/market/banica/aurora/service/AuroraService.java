package com.market.banica.aurora.service;

import com.aurora.Aurora;
import com.aurora.AuroraServiceGrpc;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;

@Service
public class AuroraService extends AuroraServiceGrpc.AuroraServiceImplBase {

    @Override
    public void subscribe(Aurora.AuroraSubscribeRequest request, StreamObserver<Aurora.AuroraSubscribeResponse> responseObserver) {
        super.subscribe(request, responseObserver);
    }

    @Override
    public void request(Aurora.AuroraRequest request, StreamObserver<Aurora.AuroraResponse> responseObserver) {
        if (request.hasBuyGoodRequest()) {
            //TODO Buy method
        } else if (request.hasSellGoodRequest()) {
            //TODO Sell method
        }
    }
}