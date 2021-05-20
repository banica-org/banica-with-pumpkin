package com.market.banica.aurora.observer;


import com.aurora.Aurora;
import com.google.protobuf.AbstractMessage;
import com.google.protobuf.Any;
import com.orderbook.ReconnectionResponse;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

public class GenericObserver<S extends AbstractMessage> implements StreamObserver<S> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenericObserver.class);

    AtomicInteger openStreams;

    private final String client;
    private final String destinationOfMessages;
    private final String typeOfMessage;

    private final StreamObserver<Aurora.AuroraResponse> forwardResponse;

    public GenericObserver(String client, StreamObserver<Aurora.AuroraResponse> forwardResponse, AtomicInteger openStreams,
                           String destinationOfMessages, String typeOfMessage) {
        this.client = client;
        this.forwardResponse = forwardResponse;
        this.openStreams = openStreams;
        this.destinationOfMessages = destinationOfMessages;
        this.typeOfMessage = typeOfMessage;
    }

    @Override
    public void onNext(S response) {
        LOGGER.debug("Forwarding response to client {}", client);
        Aurora.AuroraResponse wrapResponse = this.wrapResponse(response);

        synchronized (forwardResponse) {
            forwardResponse.onNext(wrapResponse);
        }
    }

    @Override
    public void onError(Throwable throwable) {
        LOGGER.warn("Unable to forward.");
        LOGGER.error(throwable.getMessage());

        if (Status.fromThrowable(throwable).getCode().equals(Status.Code.UNAVAILABLE)) {
            LOGGER.warn("Publisher server: {} has suddenly became offline.", destinationOfMessages);
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
                .setItemName(typeOfMessage)
                .build();

    }

    private Aurora.AuroraResponse wrapResponse(S response) {
        return Aurora.AuroraResponse.newBuilder()
                .setMessage(Any.pack(response))
                .build();
    }

    private Aurora.AuroraResponse wrapReconnect(ReconnectionResponse reconnectionResponse) {
        return Aurora.AuroraResponse.newBuilder()
                .setMessage(Any.pack(reconnectionResponse))
                .build();
    }
}
