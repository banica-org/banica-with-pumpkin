package com.market.banica.order.book;

import com.market.MarketServiceGrpc;
import com.market.MarketDataRequest;
import com.market.TickResponse;

import io.grpc.ConnectivityState;
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

    private static final int FAILED_ATTEMPTS = 0;
    private static final long DEFAULT_WAIT_TIME_IN_MILLI = 1000;
    public static final int FAILED_ATTEMPTS_LIMIT = 11;

    private final ItemMarket itemMarket;
    private final ManagedChannel managedChannel;
    private static final Logger LOGGER = LogManager.getLogger(MarketDataClient.class);

    @Autowired
    MarketDataClient(ItemMarket itemMarket,
                     @Value("${aurora.server.host}") final String host,
                     @Value("${aurora.server.port}") final int port) throws InterruptedException {
        managedChannel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        this.itemMarket = itemMarket;

        tryReconnect(managedChannel);

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

    private void tryReconnect(ManagedChannel channel) {
        int failedAttempts = FAILED_ATTEMPTS;
        long timeToWait = DEFAULT_WAIT_TIME_IN_MILLI;

        while (!managedChannel.getState(true).equals(ConnectivityState.READY)) {
            try {
                Thread.sleep(timeToWait);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (failedAttempts < FAILED_ATTEMPTS_LIMIT) {

                failedAttempts++;
                timeToWait *= 2;

                LOGGER.info("Attempt number : " + failedAttempts + " failed to recconnect!");
                LOGGER.info("Next attempt will execute in : " + timeToWait + " milliseconds");
            }else{
                failedAttempts++;
                LOGGER.info("Attempts to recconnect : " + failedAttempts);
                LOGGER.info("Next attempt will execute in : " + timeToWait + " milliseconds");
            }
        }
        managedChannel.notifyWhenStateChanged(ConnectivityState.READY, () -> {
            tryReconnect(managedChannel);
        });
    }
}
