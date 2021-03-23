package com.market.banica.aurora.manager;

import com.aurora.Aurora;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AuroraSubscriptionManager {
    public static final String DELIMITER = "/";
    public static final String ASTERISK = "*";
    public static final String EUROPE_HEADER = "Europe";
    public static final String ASIA_HEADER = "Asia";
    public static final String AMERICA_HEADER = "America";
    public static final String ORDER_BOOK_HEADER = "OrderBook";


    private final MarketSubscriptionManager marketSubscriptionManager;
    private final OrderBookSubscriptionManager orderBookSubscriptionManager;

    @Autowired
    public AuroraSubscriptionManager(MarketSubscriptionManager marketSubscriptionManager, OrderBookSubscriptionManager orderBookSubscriptionManager) {
        this.marketSubscriptionManager = marketSubscriptionManager;
        this.orderBookSubscriptionManager = orderBookSubscriptionManager;
    }

    public void subscribe(Aurora.AuroraRequest request, StreamObserver<Aurora.AuroraResponse> responseObserver) {

        String header = extractHeader(request);

        if (!isValidRequest(header, responseObserver)) {
            return;
        }

        if (header.equalsIgnoreCase(ORDER_BOOK_HEADER)) {
            this.orderBookSubscriptionManager.subscribeForOrderBookUpdate(request, responseObserver);
        } else {
            marketSubscriptionManager.subscribeForGood(request, responseObserver);
        }
    }

    private String extractHeader(Aurora.AuroraRequest request) {
        return request.getTopic().split(DELIMITER)[0];

    }

    private boolean isValidRequest(String header, StreamObserver<Aurora.AuroraResponse> responseObserver) {


        if (!isValidHeader(header)) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Request has invalid header data!").asRuntimeException());
            return false;
        }
        return true;
    }

    private boolean isValidHeader(String header) {
        return header.equalsIgnoreCase(EUROPE_HEADER) ||
                header.equalsIgnoreCase(ASIA_HEADER) ||
                header.equalsIgnoreCase(AMERICA_HEADER) ||
                header.equalsIgnoreCase(ORDER_BOOK_HEADER) ||
                header.equals(ASTERISK);
    }
}