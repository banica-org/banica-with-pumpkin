package com.market.banica.order.book;

import com.aurora.Aurora;
import com.aurora.AuroraServiceGrpc;

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

    private void start() throws InterruptedException {
        for (String product : itemMarket.getAllProductNames()) {
            final AuroraServiceGrpc.AuroraServiceStub asynchronousStub = AuroraServiceGrpc.newStub(managedChannel);
            final Aurora.AuroraRequest request = Aurora.AuroraRequest.newBuilder()
                    .setTopic("market/" + product)
                    .setClientId("OrderBook")
                    .build();
            LOGGER.info("Start gathering product data.");
            asynchronousStub.subscribe(request, new StreamObserver<Aurora.AuroraResponse>() {

                @Override
                public void onNext(Aurora.AuroraResponse response) {
                    if (response.hasTickResponse()) {
                        TickResponse tickResponse = response.getTickResponse();
                        Item item = new Item();
                        item.setPrice(tickResponse.getPrice());
                        item.setQuantity((int) tickResponse.getQuantity());
                        item.getItemIDs().add(new Item.ItemID("1", tickResponse.getOrigin().toString()));
                        itemMarket.getAllItemsByName(tickResponse.getGoodName()).add(item);

                        LOGGER.info("Products data updated!");
                    } else {
                        throw new RuntimeException("Content is not correct!");
                    }
                }

                @Override
                public void onError(final Throwable throwable) {
                    LOGGER.warn("Unable to request");
                    LOGGER.info(throwable);
                    throwable.printStackTrace();
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
            } else {
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
