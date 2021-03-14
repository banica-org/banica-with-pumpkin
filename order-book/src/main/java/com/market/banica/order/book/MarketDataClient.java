package com.market.banica.order.book;

import epam.market.banica.order.book.grpc.MarketDataRequest;
import epam.market.banica.order.book.grpc.MarketDataServiceGrpc;
import epam.market.banica.order.book.grpc.TickResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;

@Service
@RequiredArgsConstructor
public class MarketDataClient {

    private final ItemMarket itemMarket;
    private final ManagedChannel managedChannel;

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

                    Item item = new Item();
                    item.setPrice(response.getPrice());
                    item.setQuantity((int) response.getQuantity());
                    item.getItemIDs().add(new Item.ItemID("1", response.getOrigin().toString()));

                    itemMarket.getAllItemsByName(response.getItemName()).add(item);
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
//        exponential backoff! check


    @PreDestroy
    private void stop() {
        managedChannel.shutdownNow();
    }
}

