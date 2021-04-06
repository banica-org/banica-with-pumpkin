package com.market.banica.aurora.observer;

import com.aurora.Aurora;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;


public class AuroraObserver implements StreamObserver<Aurora.AuroraResponse> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuroraObserver.class);

    AtomicInteger openStreams;

    private final Aurora.AuroraRequest request;
    private final StreamObserver<Aurora.AuroraResponse> forwardResponse;

    public AuroraObserver(Aurora.AuroraRequest request, StreamObserver<Aurora.AuroraResponse> forwardResponse, AtomicInteger openStreams) {
        this.request = request;
        this.openStreams = openStreams;
        this.forwardResponse = forwardResponse;
    }

    @Override
    public void onNext(Aurora.AuroraResponse response) {
        LOGGER.debug("Forwarding response to client {}", request.getClientId());
        synchronized (forwardResponse){
            forwardResponse.onNext(response);
        }
    }


    @Override
    public void onError(Throwable throwable) {
        LOGGER.warn("Unable to forward.");
        LOGGER.error(throwable.getMessage());

        if (openStreams.decrementAndGet() == 0) {
            forwardResponse.onCompleted();
        }
    }

    @Override
    public void onCompleted() {
        LOGGER.info("Completing stream request for client {}", request.getClientId());
        if (openStreams.decrementAndGet() == 0) {
            forwardResponse.onCompleted();
        }
    }
}
