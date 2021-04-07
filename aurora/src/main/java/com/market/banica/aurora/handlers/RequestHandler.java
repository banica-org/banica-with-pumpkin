package com.market.banica.aurora.handlers;

import com.aurora.Aurora;
import com.market.banica.aurora.mapper.RequestMapper;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.management.ServiceNotFoundException;
import java.rmi.NoSuchObjectException;

@Service
public class RequestHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestHandler.class);

    private final RequestMapper requestMapper;

    @Autowired
    public RequestHandler(RequestMapper requestMapper) {
        this.requestMapper = requestMapper;
    }

    public void handleRequest(Aurora.AuroraRequest request, StreamObserver<Aurora.AuroraResponse> responseObserver) {
        LOGGER.info("Handling request from client {}", request.getClientId());
        Aurora.AuroraResponse response;

        try {
            response = requestMapper.renderRequest(request);
        } catch (NoSuchObjectException | IllegalArgumentException e) {
            LOGGER.warn("Unsupported message have reached aurora.");
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Request from client is invalid : "+e.getMessage())
                    .withCause(e.getCause())
                    .asException());
            return;
        } catch (ServiceNotFoundException e) {
            LOGGER.warn("Unsupported message have reached aurora.");
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription("Request from client is invalid : " + e.getMessage())
                    .withCause(e.getCause())
                    .asException());
            return;
        } catch (Exception e) {
            LOGGER.warn("Unable to forward.");
            LOGGER.error(e.getMessage());
            responseObserver.onError(Status.ABORTED
                    .withDescription("Receiver stopped sending message with description : " + e.getMessage())
                    .withCause(e.getCause())
                    .asException());
            return;
        }

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

}
