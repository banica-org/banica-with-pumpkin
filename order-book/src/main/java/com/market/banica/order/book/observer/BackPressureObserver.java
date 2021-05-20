package com.market.banica.order.book.observer;

import com.aurora.Aurora;
import com.google.protobuf.InvalidProtocolBufferException;
import com.market.MarketDataRequest;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BackPressureObserver implements StreamObserver<Aurora.AuroraResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackPressureObserver.class);

    @Override
    public void onNext(Aurora.AuroraResponse response) {
        try {
            String orderBookIdentifier = response.getMessage().unpack(MarketDataRequest.class).getClientId().split("/")[0];
            if (response.getMessage().unpack(MarketDataRequest.class).getClientId().split("/")[1].equalsIgnoreCase("on")) {
                LOGGER.info("Backpressure Activated for orderbook with gRPC port --> " + orderBookIdentifier);
            } else {
                LOGGER.info("Backpressure Deactivated for orderbook with gRPC port --> " + orderBookIdentifier);
            }
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onError(Throwable throwable) {
        LOGGER.warn("Unable to activate Backpressure.");
        LOGGER.error(throwable.getMessage());
    }

    @Override
    public void onCompleted() {
        LOGGER.debug("onCompleted called.");
    }
}

