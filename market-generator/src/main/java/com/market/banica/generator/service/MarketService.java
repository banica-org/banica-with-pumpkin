package com.market.banica.generator.service;

import com.market.CatalogueRequest;
import com.market.CatalogueResponse;
import com.market.MarketDataRequest;
import com.market.TickResponse;
import io.grpc.stub.StreamObserver;

public interface MarketService {
    void subscribeForItem(MarketDataRequest request, StreamObserver<TickResponse> responseObserver);
    void requestCatalogue(CatalogueRequest request, StreamObserver<CatalogueResponse> responseObserver);
}
