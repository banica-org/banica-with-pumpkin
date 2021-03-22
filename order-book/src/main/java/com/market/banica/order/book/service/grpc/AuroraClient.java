package com.market.banica.order.book.service.grpc;

import com.aurora.Aurora;
import com.aurora.AuroraServiceGrpc;
import com.market.TickResponse;
import com.market.banica.common.ChannelRPCConfig;
import com.market.banica.order.book.exception.IncorrectResponseException;
import com.market.banica.order.book.exception.TrackingException;
import com.market.banica.order.book.model.Item;
import com.market.banica.order.book.model.ItemMarket;
import io.grpc.Context;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class AuroraClient {

    private final ItemMarket itemMarket;
    private final ManagedChannel managedChannel;
    private final Map<String, Context.CancellableContext> cancellableStubs;
    private static final Logger LOGGER = LogManager.getLogger(AuroraClient.class);

    @Autowired
    AuroraClient(ItemMarket itemMarket,
                 @Value("${aurora.server.host}") final String host,
                 @Value("${aurora.server.port}") final int port) {

        managedChannel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .defaultServiceConfig(ChannelRPCConfig.getInstance().getServiceConfig())
                .enableRetry()
                .build();

        this.itemMarket = itemMarket;
        this.cancellableStubs = new ConcurrentHashMap<>();

    }

    public void updateItems(List<String> requestedItems, String clientId) throws TrackingException {
        List<String> stoppedItems = new ArrayList<>(itemMarket.getItemNameSet());
        List<String> startedItems = new ArrayList<>(requestedItems);

        stoppedItems.removeAll(requestedItems);
        startedItems.removeAll(itemMarket.getItemNameSet());

        startTrackingItems(startedItems, clientId);
        stopTrackingItems(stoppedItems);

    }

    private void startTrackingItems(List<String> trackedItems, String clientId) throws TrackingException {
        for (String product : trackedItems) {

            if (cancellableStubs.containsKey(product)) {
                throw new TrackingException("Item is already being tracked!");
            }

            final Aurora.AuroraRequest request = Aurora.AuroraRequest.newBuilder()
                    .setTopic("market/" + product)
                    .setClientId(clientId)
                    .build();

            LOGGER.info("Start gathering product data.");

            Context.CancellableContext withCancellation = Context.current().withCancellation();
            cancellableStubs.put(product, withCancellation);
            withCancellation.run(() -> startMarketStream(request));

        }
    }

    private void startMarketStream(Aurora.AuroraRequest request) {
        final AuroraServiceGrpc.AuroraServiceStub asynchronousStub = AuroraServiceGrpc.newStub(managedChannel);

        asynchronousStub.subscribe(request, new StreamObserver<Aurora.AuroraResponse>() {

            @Override
            public void onNext(Aurora.AuroraResponse response) {
                if (response.hasTickResponse()) {
                    TickResponse tickResponse = response.getTickResponse();

                    Item item = new Item();
                    item.setPrice(tickResponse.getPrice());
                    item.setQuantity(tickResponse.getQuantity());
                    item.setOrigin(tickResponse.getOrigin());

                    itemMarket.getItemSetByName(tickResponse.getGoodName()).add(item);

                    LOGGER.info("Products data updated!");
                } else {
                    throw new IncorrectResponseException("Response is not correct!");
                }
            }

            @Override
            public void onError(final Throwable throwable) {
                LOGGER.warn("Unable to request");
                LOGGER.error(throwable.getMessage());
                throwable.printStackTrace();
            }

            @Override
            public void onCompleted() {
                LOGGER.info("Market data gathered");
            }

        });

    }

    private void stopTrackingItems(List<String> untrackedItems) throws TrackingException {
        for (String product : untrackedItems) {

            if (!cancellableStubs.containsKey(product)) {
                throw new TrackingException("Item is not being tracked!");
            }

            Context.CancellableContext cancelledStub = cancellableStubs.remove(product);
            cancelledStub.cancel(null);

        }
    }

    @PreDestroy
    private void stop() throws InterruptedException {
        managedChannel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        LOGGER.info("Server is terminated!");
    }

}
