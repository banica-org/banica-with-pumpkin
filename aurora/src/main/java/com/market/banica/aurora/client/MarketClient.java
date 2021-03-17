package com.market.banica.aurora.client;

import com.market.CatalogueRequest;
import com.market.CatalogueResponse;
import com.market.MarketDataRequest;
import com.market.MarketServiceGrpc;
import com.market.banica.aurora.channel.MarketChannelManager;
import com.market.banica.aurora.observers.MarketTickResponseObserver;
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

        MarketDataRequest marketDataRequest = MarketDataRequest.newBuilder().setGoodName(getGood(request)).build();

        ManagedChannel channel = marketChannelManager.getMarketChannel(getOrigin(request));

        MarketServiceGrpc.MarketServiceStub marketServiceStub = MarketServiceGrpc.newStub(channel);

        marketServiceStub.subscribeForItem(marketDataRequest,responseObserver);
    }

    public Set<String> getMarketCatalogue(String marketOrigin) {

        CatalogueResponse catalogueResponse = MarketServiceGrpc.newBlockingStub(marketChannelManager.getMarketChannel(marketOrigin))
                .requestCatalogue(CatalogueRequest.newBuilder().build());

        return new HashSet<>(catalogueResponse.getFoodItemsList());
    }

    private String getOrigin(MarketDataRequest marketDataRequest) {
        return marketDataRequest.getGoodName().split(DELIMITER)[0];
    }

    private String getGood(MarketDataRequest marketDataRequest) {
        return marketDataRequest.getGoodName().split(DELIMITER)[1];
    }

}
