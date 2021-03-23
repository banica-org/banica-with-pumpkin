package com.market.banica.aurora.client;

import com.market.CatalogueRequest;
import com.market.CatalogueResponse;
import com.market.MarketDataRequest;
import com.market.MarketServiceGrpc;
import com.market.banica.aurora.channel.MarketChannelManager;
import com.market.banica.aurora.observer.MarketTickResponseObserver;
import io.grpc.ManagedChannel;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class MarketClient {
    public static final String DELIMITER = "/";

    private final MarketChannelManager marketChannelManager;

    public void subscribeForMarketGood(MarketDataRequest request, MarketTickResponseObserver responseObserver) {

        MarketDataRequest marketDataRequest = MarketDataRequest.newBuilder().setGoodName(request.getGoodName()).build();

        ManagedChannel channel = marketChannelManager.getMarketChannel(getOrigin(request));

        MarketServiceGrpc.MarketServiceStub marketServiceStub = MarketServiceGrpc.newStub(channel);

        marketServiceStub.subscribeForItem(marketDataRequest, responseObserver);
    }

    public Set<String> getMarketCatalogue(MarketDataRequest request) {

        CatalogueResponse catalogueResponse = MarketServiceGrpc.newBlockingStub(marketChannelManager.getMarketChannel(getOrigin(request)))
                .requestCatalogue(CatalogueRequest.newBuilder().setClientId(request.getClientId()).setMarketOrigin(getOrigin(request)).build());

        return new HashSet<>(catalogueResponse.getFoodItemsList());
    }

    private String getOrigin(MarketDataRequest marketDataRequest) {
        return marketDataRequest.getGoodName().split(DELIMITER)[0];
    }

    private String getGood(MarketDataRequest marketDataRequest) {
        return marketDataRequest.getGoodName().split(DELIMITER)[1];
    }

}
