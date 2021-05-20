package com.market.banica.aurora.backpressure;

import com.aurora.Aurora;
import com.google.protobuf.AbstractMessage;
import com.google.protobuf.Any;
import com.market.MarketDataRequest;
import com.market.banica.aurora.observer.GenericObserver;

import io.grpc.stub.StreamObserver;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@NoArgsConstructor
@Setter
@Getter
public class BackPressureManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(BackPressureManager.class);

    private int numberOfMessagesToBeRequested = 1;

    private final Map<String, Set<GenericObserver<? extends AbstractMessage, ? extends AbstractMessage>>> marketTickObservers = new ConcurrentHashMap<>();

    public void activateBackPressure(String orderBookIdentifier, StreamObserver<Aurora.AuroraResponse> responseObserver) {
        LOGGER.debug("Activating backpressure for orderbook with gRPC port --> {}", orderBookIdentifier);
        marketTickObservers.get(orderBookIdentifier).forEach(tick -> tick.setBackPressureForTick(true));
        notifyBackpressureObserver(orderBookIdentifier + "/on", responseObserver);
    }

    public void deActivateBackPressure(String orderBookIdentifier, StreamObserver<Aurora.AuroraResponse> responseObserver) {
        LOGGER.debug("Deactivating backpressure for orderbook with gRPC port --> {}", orderBookIdentifier);
        for (GenericObserver marketTickObserver : marketTickObservers.get(orderBookIdentifier)) {
            marketTickObserver.setBackPressureForTick(false);
            marketTickObserver.getCountDownLatch().countDown();
        }
        notifyBackpressureObserver(orderBookIdentifier + "/off", responseObserver);
    }

    public void addMarketTickObserver(GenericObserver marketTickObserver, String orderBookIdentifier) {
        marketTickObservers.putIfAbsent(orderBookIdentifier, ConcurrentHashMap.newKeySet());
        marketTickObservers.get(orderBookIdentifier).add(marketTickObserver);
    }

    private void notifyBackpressureObserver(String orderBookIdentifier, StreamObserver<Aurora.AuroraResponse> responseObserver) {
        LOGGER.debug("Notifying backpressure observer --> {}", responseObserver);

        MarketDataRequest marketDataRequest = MarketDataRequest.newBuilder()
                .setClientId(orderBookIdentifier)
                .build();

        responseObserver.onNext(Aurora.AuroraResponse.newBuilder().setMessage(Any.pack(marketDataRequest)).build());
    }
}
