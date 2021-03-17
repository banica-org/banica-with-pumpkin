package com.market.banica.order.book;

import com.market.MarketDataRequest;
import com.market.MarketServiceGrpc;
import com.market.TickResponse;
import com.market.banica.common.ChannelRPCConfig;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
    private static final Logger LOGGER = LogManager.getLogger(MarketDataClient.class);

    @Autowired
    MarketDataClient(ItemMarket itemMarket,
                     @Value("${aurora.server.host}") final String host,
                     @Value("${aurora.server.port}") final int port) {

        managedChannel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .defaultServiceConfig(ChannelRPCConfig.getRetryingServiceConfig())
                .enableRetry()
                .build();

        this.itemMarket = itemMarket;

    }

    @PostConstruct
    private void start() {
        for (String product : itemMarket.getAllProductNames()) {
            final MarketServiceGrpc.MarketServiceStub asynchronousStub = MarketServiceGrpc.newStub(managedChannel);
            final MarketDataRequest request = MarketDataRequest.newBuilder()
                    .setGoodName(product)
                    .build();
            LOGGER.info("Start gathering product data.");
            asynchronousStub.subscribeForItem(request, new StreamObserver<TickResponse>() {

                @Override
                public void onNext(TickResponse response) {
                    Item item = new Item();
                    item.setPrice(response.getPrice());
                    item.setQuantity((int) response.getQuantity());
                    item.getItemIDs().add(new Item.ItemID("1", response.getOrigin().toString()));
                    LOGGER.info("Products data updated!");

                    itemMarket.getAllItemsByName(response.getGoodName()).add(item);
                }

                @Override
                public void onError(final Throwable throwable) {
                    LOGGER.warn("Unable to request");
                }

                @Override
                public void onCompleted() {
                    LOGGER.info("Market data gathered");
                }
            });
        }
    }

    @PreDestroy
    private void stop() throws InterruptedException {
        managedChannel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        LOGGER.info("Server is terminated!");
    }

}
