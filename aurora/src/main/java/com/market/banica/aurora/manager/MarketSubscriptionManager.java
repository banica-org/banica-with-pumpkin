package com.market.banica.aurora.manager;

import com.aurora.Aurora;
import com.market.MarketDataRequest;
import com.market.TickResponse;
import com.market.banica.aurora.channel.MarketChannelManager;
import com.market.banica.aurora.client.MarketClient;
import com.market.banica.aurora.observer.MarketTickResponseObserver;
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
public class MarketSubscriptionManager {

    public static final String DELIMITER = "/";
    public static final String ASTERISK = "*";

    private MarketChannelManager marketChannelManager;

    private MarketClient marketClient;

    private HashMap<StreamObserver<?>, StreamObserver<Aurora.AuroraResponse>> observersMap = new HashMap<>();

    private ReentrantLock lock;

    @Autowired
    public MarketSubscriptionManager(MarketChannelManager marketChannelManager, MarketClient marketClient) {
        this.marketChannelManager = marketChannelManager;
        this.marketClient = marketClient;
        this.lock = new ReentrantLock(true);
    }


    public void subscribeForGood(Aurora.AuroraRequest request, StreamObserver<Aurora.AuroraResponse> responseObserver) {

        String requestOriginName = extractMarketOrigin(request.getTopic());
        String requestGoodName = extractGood(request.getTopic());

        Set<MarketDataRequest> marketDataRequests;
        Set<String> strings = marketChannelManager.getMarketChannels().keySet();
        marketDataRequests = mapWildcard(requestOriginName, strings, (marketName) -> marketName + DELIMITER + requestGoodName,
                request.getClientId(), request.getTopic());

        marketDataRequests.forEach(marketDataRequest -> {

            String marketOriginName = extractMarketOrigin(marketDataRequest.getGoodName());
            String marketGoodName = extractGood(marketDataRequest.getGoodName());

            Set<MarketDataRequest> newMarketDataRequests;

            newMarketDataRequests = mapWildcard(marketGoodName, marketClient.getMarketCatalogue(marketOriginName),
                    (goodName) -> marketOriginName + DELIMITER + goodName, marketDataRequest.getClientId(), marketDataRequest.getGoodName());

            newMarketDataRequests.forEach(dataRequest -> {
                MarketTickResponseObserver marketTickResponseObserver = new MarketTickResponseObserver(this);
                observersMap.put(marketTickResponseObserver, responseObserver);
                marketClient.subscribeForMarketGood(dataRequest, marketTickResponseObserver);
            });


        });

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

    private String extractMarketOrigin(String topic) {
        return topic.split(DELIMITER)[0];
    }

    private String extractGood(String topic) {
        return topic.split(DELIMITER)[1];
    }

    private Set<MarketDataRequest> mapWildcard(String possibleMarketData, Set<String> getPossibleMarketData, Function<String, String> marketDataMapping, String clientId, String goodName) {
        if (possibleMarketData.equals(ASTERISK)) {
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

}
