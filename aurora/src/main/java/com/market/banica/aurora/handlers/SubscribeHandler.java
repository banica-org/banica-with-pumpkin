package com.market.banica.aurora.handlers;

import com.aurora.Aurora;
import com.market.banica.aurora.mapper.SubscribeMapper;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SubscribeHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubscribeHandler.class);

    private final SubscribeMapper subscribeMapper;

    @Autowired
    public SubscribeHandler(SubscribeMapper subscribeMapper) {
        this.subscribeMapper = subscribeMapper;
    }

    public void handleSubscribe(Aurora.AuroraRequest request, StreamObserver<Aurora.AuroraResponse> responseObserver) {
        LOGGER.info("Handling subscribe from client {}", request.getClientId());
        subscribeMapper.renderSubscribe(request, responseObserver);
    }


}