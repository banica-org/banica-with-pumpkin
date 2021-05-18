package com.market.banica.aurora.observer;

import com.aurora.Aurora;
import com.google.protobuf.Any;
import com.market.TickResponse;
import com.orderbook.ReconnectionResponse;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

public class MarketTickObserver implements StreamObserver<TickResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MarketTickObserver.class);

    AtomicInteger openStreams;

    private final String client;
    private final String destinationOfMessages;
    private final String item;

    private final StreamObserver<Aurora.AuroraResponse> forwardResponse;

    public MarketTickObserver(String client, StreamObserver<Aurora.AuroraResponse> forwardResponse, AtomicInteger openStreams,
                              String destinationOfMessages, String item) {
        this.client = client;
        this.forwardResponse = forwardResponse;
        this.openStreams = openStreams;
        this.destinationOfMessages = destinationOfMessages;
        this.item = item;
    }

    @Override
    public void onNext(TickResponse tickResponse) {
        LOGGER.debug("Forwarding response to client {}", client);
        Aurora.AuroraResponse response = this.wrapResponse(tickResponse);

        synchronized (forwardResponse) {
            forwardResponse.onNext(response);
        }
    }

    @Override
    public void onError(Throwable throwable) {
        LOGGER.warn("Unable to forward.");
        LOGGER.error(throwable.getMessage());

        if (Status.fromThrowable(throwable).getCode().equals(Status.Code.UNAVAILABLE)) {
            LOGGER.warn("Market server: {} has suddenly became offline.", destinationOfMessages);
            synchronized (forwardResponse) {
                forwardResponse.onNext(this.wrapReconnect(buildReconnect()));
            }
        }
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

    private ReconnectionResponse buildReconnect() {
        return ReconnectionResponse.newBuilder()
                .setClientId(client)
                .setDestination(destinationOfMessages)
                .setItemName(item)
                .build();

    }

    private Aurora.AuroraResponse wrapResponse(TickResponse tickResponse) {
        return Aurora.AuroraResponse.newBuilder()
                .setMessage(Any.pack(tickResponse))
                .build();
    }

    private Aurora.AuroraResponse wrapReconnect(ReconnectionResponse reconnectionResponse) {
        return Aurora.AuroraResponse.newBuilder()
                .setMessage(Any.pack(reconnectionResponse))
                .build();
    }
}
