package com.market.banica.order.book;

import com.aurora.Aurora;
import com.aurora.AuroraServiceGrpc;

import com.market.TickResponse;
import io.grpc.ConnectivityState;
import com.market.banica.common.ChannelRPCConfig;
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

    private final ItemMarket itemMarket;
    private final ManagedChannel managedChannel;
    private static final Logger LOGGER = LogManager.getLogger(MarketDataClient.class);

    @Autowired
    MarketDataClient(ItemMarket itemMarket,
                     @Value("${aurora.server.host}") final String host,
                     @Value("${aurora.server.port}") final int port) {

        managedChannel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .defaultServiceConfig(ChannelRPCConfig.getInstance().getServiceConfig())
                .enableRetry()
                .build();

        this.itemMarket = itemMarket;

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
}
