package com.market.banica.aurora.handlers;

import com.aurora.Aurora;
import com.market.banica.aurora.mapper.SubscribeMapper;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;

@Service
public class SubscribeHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubscribeHandler.class);

    private final SubscribeMapper subscribeMapper;

    @Autowired
    public SubscribeHandler(SubscribeMapper subscribeMapper) {
        this.subscribeMapper = subscribeMapper;
    }

    public void handleSubscribe(Aurora.AuroraRequest request, StreamObserver<Aurora.AuroraResponse> responseObserver) {
        try {
            LOGGER.info("Handling subscribe from client {}", request.getClientId());
            subscribeMapper.renderSubscribe(request, responseObserver);
        } catch (NoSuchMethodException e) {
            LOGGER.warn("There is no such method.");
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription("Subscribe is invalid: " + e.getMessage())
                    .withCause(e.getCause())
                    .asException());
        } catch (InvocationTargetException | IllegalAccessException e) {
            LOGGER.warn("Couldn't invoke method.");
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription("Subscribe is invalid: " + e.getMessage())
                    .withCause(e.getCause())
                    .asException());
        } catch (Exception e) {
            LOGGER.warn("Unable to forward.");
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Subscribtion stopped with description: " + e.getMessage())
                    .withCause(e.getCause())
                    .asException());
        }
    }

}