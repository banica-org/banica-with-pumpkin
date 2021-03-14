package com.market.banica.order.book;

import epam.market.banica.order.book.grpc.MarketDataRequest;
import epam.market.banica.order.book.grpc.MarketDataServiceGrpc;
import epam.market.banica.order.book.grpc.TickResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.TimeUnit;

@Service

public class MarketDataClient {

    private final ItemMarket itemMarket;
    private final ManagedChannel managedChannel;
    private static final Logger LOGGER = LoggerFactory.getLogger(MarketDataClient.class);

    @Autowired
    MarketDataClient(ItemMarket itemMarket,
                     @Value("${grpc.server.host}") final String host,
                     @Value("${grpc.server.port}") final int port) {
        managedChannel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        this.itemMarket = itemMarket;
    }

    @PostConstruct
    private void start() {
        for (String product : itemMarket.getAllProductNames()) {
            final MarketDataServiceGrpc.MarketDataServiceStub asynchronousStub = MarketDataServiceGrpc.newStub(managedChannel);
            final MarketDataRequest request = MarketDataRequest.newBuilder()
                    .setItemName(product)
                    .build();
            LOGGER.debug("Start gathering product data.");
            asynchronousStub.getMarketData(request, new StreamObserver<TickResponse>() {

                @Override
                public void onNext(TickResponse response) {
                    Item item = new Item();
                    item.setPrice(response.getPrice());
                    item.setQuantity((int) response.getQuantity());
                    item.getItemIDs().add(new Item.ItemID("1", response.getOrigin().toString()));
                    LOGGER.debug("Products data updated!");

                    itemMarket.getAllItemsByName(response.getItemName()).add(item);
                }

                @Override
                public void onError(final Throwable throwable) {
                    LOGGER.debug("Unable to request");
                }

                @Override
                public void onCompleted() {
                    LOGGER.debug("Market data gathered");
                }
            });
        }
    }
//        exponential backoff! check

    @PreDestroy
    private void stop() throws InterruptedException {
        managedChannel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        LOGGER.debug("Server is terminated!");
    }
}
