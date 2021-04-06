package com.market.banica.aurora.handlers;

import com.aurora.Aurora;
import com.aurora.AuroraServiceGrpc;
import com.market.banica.aurora.config.ChannelManager;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class RequestHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestHandler.class);

    private final ChannelManager channels;

    @Autowired
    public RequestHandler(ChannelManager channelManager) {
        channels = channelManager;
    }

    public void handleRequest(Aurora.AuroraRequest request, StreamObserver<Aurora.AuroraResponse> responseObserver) {
        LOGGER.info("Handling request from client {}", request.getClientId());
        Optional<ManagedChannel> channelByKey = channels.getChannelByKey(request.getTopic().split("/")[0]);
        if (!channelByKey.isPresent()) {
            LOGGER.warn("Unsupported message have reached aurora.");
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(request.getTopic().split("/")[0] + " channel not available at the moment.")
                    .asException());
            return;
        }
        AuroraServiceGrpc.AuroraServiceBlockingStub stub = generateAuroraStub(channelByKey.get());

        processRequest(request, responseObserver, stub);
    }

    private void processRequest(Aurora.AuroraRequest request, StreamObserver<Aurora.AuroraResponse> responseObserver,
                                AuroraServiceGrpc.AuroraServiceBlockingStub stub) {
        Aurora.AuroraResponse response;

        try {
            response = stub.request(request);
        } catch (Exception e) {
            LOGGER.warn("Unable to forward.");
            LOGGER.error(e.getMessage());
            responseObserver.onError(e.getCause());
            return;
        }


        responseObserver.onNext(response);
        LOGGER.info("Completing request for client {}", request.getClientId());
        responseObserver.onCompleted();
    }

    protected AuroraServiceGrpc.AuroraServiceBlockingStub generateAuroraStub(ManagedChannel channelByKey) {
        LOGGER.debug("Generating blocking stub.");
        return AuroraServiceGrpc.newBlockingStub(channelByKey);
    }
}
