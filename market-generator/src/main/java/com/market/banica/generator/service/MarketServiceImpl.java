package com.market.banica.generator.service;

import com.market.CatalogueRequest;
import com.market.CatalogueResponse;
import com.market.MarketDataRequest;
import com.market.MarketServiceGrpc;
import com.market.TickResponse;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;

@Service
public class MarketServiceImpl extends MarketServiceGrpc.MarketServiceImplBase implements MarketService {

    @Override
    public void subscribeForItem(MarketDataRequest request, StreamObserver<TickResponse> responseObserver) {
        super.subscribeForItem(request, responseObserver);
    }

    @Override
    public void requestCatalogue(CatalogueRequest request, StreamObserver<CatalogueResponse> responseObserver) {
        super.requestCatalogue(request, responseObserver);
    }
}
