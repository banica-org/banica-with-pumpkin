package com.market.banica.aurora.observer;

import com.aurora.Aurora;
import com.google.protobuf.Any;
import com.market.MarketDataRequest;
import com.market.TickResponse;
import com.market.banica.aurora.backpressure.BackPressureManager;
import com.orderbook.ReconnectionResponse;
import io.grpc.Status;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.ClientResponseObserver;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class MarketTickObserver implements ClientResponseObserver<MarketDataRequest, TickResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MarketTickObserver.class);

    private final AtomicInteger openStreams;

    private final String client;
    private final String destinationOfMessages;
    private final String item;
    private final String orderBookIdentifier;

    private final StreamObserver<Aurora.AuroraResponse> forwardResponse;

    private final MarketDataRequest marketDataRequest;

    private final BackPressureManager backPressureManager;

    private final AtomicBoolean backPressureActivated = new AtomicBoolean(false);

    private ClientCallStreamObserver<Aurora.AuroraRequest> requestStream;

    private CountDownLatch countDownLatch = new CountDownLatch(1);

    public MarketTickObserver(String client, StreamObserver<Aurora.AuroraResponse> forwardResponse, AtomicInteger openStreams, String destinationOfMessages,
                              String item, MarketDataRequest marketDataRequest, BackPressureManager backPressureManager, String orderBookIdentifier) {
        this.client = client;
        this.forwardResponse = forwardResponse;
        this.openStreams = openStreams;
        this.destinationOfMessages = destinationOfMessages;
        this.item = item;
        this.marketDataRequest = marketDataRequest;
        this.backPressureManager = backPressureManager;
        this.orderBookIdentifier = orderBookIdentifier;
    }

    @Override
    public void beforeStart(ClientCallStreamObserver requestStream) {
        LOGGER.debug("Initializing before start");
        this.requestStream = requestStream;
        backPressureManager.addMarketTickObserver(this, orderBookIdentifier);

        requestStream.disableAutoRequestWithInitial(1);

        requestStream.setOnReadyHandler(() -> {
            if (requestStream.isReady()) {
                requestStream.onNext(marketDataRequest);
            }
        });
    }

    @Override
    public void onNext(TickResponse tickResponse) {
        LOGGER.debug("Forwarding response to client {}", client);

        Aurora.AuroraResponse response = this.wrapResponse(tickResponse);

        synchronized (forwardResponse) {
            forwardResponse.onNext(response);
            if (backPressureActivated.get()) {
                try {
                    LOGGER.info("Backpressure activated by orderbook with gRPC port: {}, for item --> {}", orderBookIdentifier, item);
                    countDownLatch.await();
                    countDownLatch = new CountDownLatch(1);
                    LOGGER.info("Backpressure deactivated by orderbook with gRPC port: {}, for item --> {}", orderBookIdentifier, item);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            requestStream.request(backPressureManager.getNumberOfMessagesToBeRequested());
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

    public CountDownLatch getCountDownLatch() {
        return countDownLatch;
    }

    public void setBackPressureForTick(boolean isOn) {
        this.backPressureActivated.set(isOn);
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
