package com.market.banica.aurora.service;

import com.aurora.Aurora;
import com.aurora.AuroraServiceGrpc;
import com.market.banica.aurora.backpressure.BackPressureManager;
import com.market.banica.aurora.handlers.SubscribeHandler;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.market.banica.aurora.handlers.RequestHandler;

import java.lang.reflect.InvocationTargetException;


@Service
public class AuroraServiceImpl extends AuroraServiceGrpc.AuroraServiceImplBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuroraServiceImpl.class);

    private final RequestHandler requestHandler;

    private final SubscribeHandler subscribeHandler;

    private final BackPressureManager backPressureManager;

    @Autowired
    public AuroraServiceImpl(RequestHandler requestHandler, SubscribeHandler subscribeHandler, BackPressureManager backPressureManager) {
        this.requestHandler = requestHandler;
        this.subscribeHandler = subscribeHandler;
        this.backPressureManager = backPressureManager;
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

    @Override
    public void backpressure(Aurora.AuroraRequest request, StreamObserver<Aurora.AuroraResponse> responseObserver) {
        if (request.getTopic().split("/")[1].equalsIgnoreCase("on")) {
            LOGGER.info("Backpressure activation request from orderbook with gRPC port: {} has been received.", request.getClientId());
            backPressureManager.activateBackPressure(request.getClientId(), responseObserver);
        } else if (request.getTopic().split("/")[1].equalsIgnoreCase("off")) {
            LOGGER.info("Backpressure deactivation request from orderbook with gRPC port: {} has been received.", request.getClientId());
            backPressureManager.deActivateBackPressure(request.getClientId(), responseObserver);
        }
    }
}
