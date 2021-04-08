package com.market.banica.aurora.observer;

import com.aurora.Aurora;
import com.google.protobuf.Any;
import com.market.TickResponse;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

public class MarketTickObserver implements StreamObserver<TickResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MarketTickObserver.class);

    AtomicInteger openStreams;

    private final String client;

    private final StreamObserver<Aurora.AuroraResponse> forwardResponse;

    public MarketTickObserver(String client, StreamObserver<Aurora.AuroraResponse> forwardResponse,AtomicInteger openStreams) {
        this.client = client;
        this.forwardResponse = forwardResponse;
        this.openStreams = openStreams;
    }

    @Override
    public void onNext(TickResponse tickResponse) {
        LOGGER.debug("Forwarding response to client {}", client);
        Aurora.AuroraResponse response = this.wrapResponse(tickResponse);

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
        LOGGER.info("Completing stream request for client {}", client);
        if (openStreams.decrementAndGet() == 0) {
            forwardResponse.onCompleted();
        }
    }

    private Aurora.AuroraResponse wrapResponse(TickResponse tickResponse){
        return Aurora.AuroraResponse.newBuilder()
                .setMessage(Any.pack(tickResponse))
                .build();
    }
}
