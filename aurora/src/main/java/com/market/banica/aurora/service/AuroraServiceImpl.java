package com.market.banica.aurora.service;

import com.aurora.Aurora;
import com.aurora.AuroraServiceGrpc;
import com.market.banica.aurora.handlers.RequestHandler;
import com.market.banica.aurora.handlers.SubscribeHandler;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class AuroraServiceImpl extends AuroraServiceGrpc.AuroraServiceImplBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuroraServiceImpl.class);

    private final RequestHandler requestHandler;

    private final SubscribeHandler subscribeHandler;

    @Autowired
    public AuroraServiceImpl(RequestHandler requestHandler, SubscribeHandler subscribeHandler) {
        this.requestHandler = requestHandler;
        this.subscribeHandler = subscribeHandler;
    }

    @Override
    public void request(Aurora.AuroraRequest request, StreamObserver<Aurora.AuroraResponse> responseObserver) {
        LOGGER.info("Accepted request from client {}", request.getClientId());
        requestHandler.handleRequest(request, responseObserver);
    }

    @Override
    public void subscribe(Aurora.AuroraRequest request, StreamObserver<Aurora.AuroraResponse> responseObserver) {
        LOGGER.info("Accepted subscribe from client {}", request.getClientId());
        subscribeHandler.handleSubscribe(request, responseObserver);
    }
}
