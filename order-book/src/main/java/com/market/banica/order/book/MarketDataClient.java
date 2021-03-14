package com.market.banica.order.book;

import epam.market.banica.order.book.grpc.MarketDataRequest;
import epam.market.banica.order.book.grpc.MarketDataServiceGrpc;
import epam.market.banica.order.book.grpc.TickResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MarketDataClient {

    private final ItemMarket itemMarket;

    private final ManagedChannel managedChannel;

    //rebuild channel
    //chek if connection is ok

    @Autowired
    MarketDataClient(ItemMarket itemMarket,
                     @Value("${grpc.server.host}") final String host,
                     @Value("${grpc.server.port}") final int port) {
        managedChannel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        this.itemMarket = itemMarket;
    }

    private void start() {
        for (String product : itemMarket.getAllProductNames()) {
            final MarketDataServiceGrpc.MarketDataServiceStub asynchronousStub = MarketDataServiceGrpc.newStub(managedChannel);
            final MarketDataRequest request = MarketDataRequest.newBuilder()
                    .setItemName(product)
                    .build();
            asynchronousStub.getMarketData(request, new StreamObserver<TickResponse>() {

                @Override
                public void onNext(TickResponse response) {
                   itemMarket.getProductSet(response.getItemName());
                    // add data from response to data structure into OrderBookServ
                    // private final Map<String, TreeSet<Item>> allItems;
                    // threadsave mechanism?
                    // data request flow ? for every product
                    // Bidirectional or servet - streaming ? when its closing
                }

                @Override
                public void onError(final Throwable throwable) {
                    System.out.println(("Unable to request"));
                }

                @Override
                public void onCompleted() {
                    System.out.println(("Market data gathered"));
                }
            });
        }
    }
        // subscribe for things that are nescessery for calculator
//        exponential backoff! check

        // for every single product ?
//        managedChannel.isShutdown();


    private void stop() {
        managedChannel.shutdownNow();
    }
}

