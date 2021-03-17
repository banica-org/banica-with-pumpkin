package com.market.banica.aurora.service;

import com.aurora.Aurora;
import com.market.MarketDataRequest;
import com.market.TickResponse;
import com.market.banica.aurora.channel.MarketChannelManager;
import com.market.banica.aurora.client.MarketClient;
import com.market.banica.aurora.observers.MarketTickResponseObserver;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class AuroraSubscriptionManager {
    public static final String DELIMITER = "/";
    public static final String ASTERISK = "*";
    public static final String EUROPE_HEADER = "Europe";
    public static final String ASIA_HEADER = "Asia";
    public static final String AMERICA_HEADER = "America";
    public static final String ORDER_BOOK_HEADER = "OrderBook";

    private MarketChannelManager marketChannelManager;

    private MarketClient marketClient;

    private HashMap<StreamObserver<?>, StreamObserver<Aurora.AuroraResponse>> observersMap = new HashMap<>();

    private ReentrantLock lock;

    @Autowired
    public AuroraSubscriptionManager(MarketChannelManager marketChannelManager, MarketClient marketClient) {
        this.marketChannelManager = marketChannelManager;
        this.marketClient = marketClient;
        this.lock = new ReentrantLock(true);
    }

    public void subscribe(Aurora.AuroraRequest request, StreamObserver<Aurora.AuroraResponse> responseObserver) {

        String header = extractHeader(request);

        if (!isValidRequest(header, responseObserver)) {
            return;
        }

        if (header.equalsIgnoreCase(ORDER_BOOK_HEADER)) {
            subscribeForOrderBookUpdate(request, responseObserver);
        } else {
            subscribeForItem(request, responseObserver);
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

    private void subscribeForItem(Aurora.AuroraRequest request, StreamObserver<Aurora.AuroraResponse> responseObserver) {

        String requestOriginName = extractMarketOrigin(request.getTopic());
        String requestGoodName = extractGood(request.getTopic());

        Set<MarketDataRequest> marketDataRequests;
        Set<String> strings = marketChannelManager.getMarketChannels().keySet();
        marketDataRequests = mapWildcard(requestOriginName, strings, (marketName) -> marketName + DELIMITER + requestGoodName, request.getClientId(), request.getTopic());

        marketDataRequests.forEach(marketDataRequest -> {

            String marketOriginName = extractMarketOrigin(marketDataRequest.getGoodName());
            String marketGoodName = extractGood(marketDataRequest.getGoodName());

            Set<MarketDataRequest> newMarketDataRequests;

            newMarketDataRequests = mapWildcard(marketGoodName, marketClient.getMarketCatalogue(marketOriginName), (goodName) -> marketOriginName + DELIMITER + goodName, marketDataRequest.getClientId(), marketDataRequest.getGoodName());

            newMarketDataRequests.forEach(dataRequest -> {
                MarketTickResponseObserver marketTickResponseObserver = new MarketTickResponseObserver(this);
                observersMap.put(marketTickResponseObserver, responseObserver);
                marketClient.subscribeForMarketGood(dataRequest, marketTickResponseObserver);
            });


        });

    }

    private String extractMarketOrigin(String topic) {
        return topic.split(DELIMITER)[0];
    }

    private String extractGood(String topic) {
        return topic.split(DELIMITER)[1];
    }

    private Set<MarketDataRequest> mapWildcard(String possibleMarketData, Set<String> getPossibleMarketData, Function<String, String> marketDataMapping, String clientId, String goodName) {
        if (possibleMarketData.equals("*")) {
            return getPossibleMarketData
                    .stream()
                    .map(data ->
                            MarketDataRequest
                                    .newBuilder()
                                    .setClientId(clientId)
                                    .setGoodName(marketDataMapping.apply(data))
                                    .build()).collect(Collectors.toSet());
        }
        return Collections.singleton(MarketDataRequest
                .newBuilder()
                .setClientId(clientId)
                .setGoodName(goodName)
                .build());

    }


    public void notifyObservers(TickResponse tickResponse, MarketTickResponseObserver marketTickResponseObserver) {
        StreamObserver<Aurora.AuroraResponse> auroraResponseStreamObserver = observersMap.get(marketTickResponseObserver);
        try {
            lock.lock();
            Aurora.AuroraResponse auroraResponse = Aurora.AuroraResponse.newBuilder().setTickResponse(tickResponse).build();
            auroraResponseStreamObserver.onNext(auroraResponse);
        } catch (StatusRuntimeException e) {
            observersMap.remove(auroraResponseStreamObserver);
        } finally {
            lock.unlock();
        }

    }

    public void unsubscribe(MarketTickResponseObserver marketTickResponseObserver) {
      observersMap.remove(marketTickResponseObserver);
    }

    private void subscribeForOrderBookUpdate(Aurora.AuroraRequest request, StreamObserver<Aurora.AuroraResponse> responseObserver) {

    }
}
