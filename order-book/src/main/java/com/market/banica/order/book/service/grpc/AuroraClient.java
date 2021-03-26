package com.market.banica.order.book.service.grpc;

import com.aurora.Aurora;
import com.aurora.AuroraServiceGrpc;
import com.market.TickResponse;
import com.market.banica.common.channel.ChannelRPCConfig;
import com.market.banica.order.book.exception.IncorrectResponseException;
import com.market.banica.order.book.exception.StoppedStreamException;
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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

    public void startSubscription(String requestedItem, String clientId) throws TrackingException {

        if (cancellableStubs.containsKey(requestedItem)) {
            throw new TrackingException("Item is already being tracked!");
        }

        final Aurora.AuroraRequest request = Aurora.AuroraRequest.newBuilder()
                .setTopic("market/" + requestedItem)
                .setClientId(clientId)
                .build();

        LOGGER.info("Start gathering product data.");

        Context.CancellableContext withCancellation = Context.current().withCancellation();
        cancellableStubs.put(requestedItem, withCancellation);
        itemMarket.addTrackedItem(requestedItem);
        withCancellation.run(() -> startMarketStream(request));
    }

    public void stopSubscription(String requestedItem, String clientId) throws TrackingException {

        if (!cancellableStubs.containsKey(requestedItem)) {
            throw new TrackingException("Item is not being tracked!");
        }

        Context.CancellableContext cancelledStub = cancellableStubs.remove(requestedItem);
        cancelledStub.cancel(new StoppedStreamException("Stopped tracking stream for: " + requestedItem));
        itemMarket.removeUntrackedItem(requestedItem);
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

                    Optional<Set<Item>> itemSet = itemMarket.getItemSetByName(tickResponse.getGoodName());
                    if (itemSet.isPresent()) {
                        itemSet.get().add(item);
                    } else {
                        LOGGER.error("Item: {} is not being tracked and cannot be added to itemMarket!",
                                tickResponse.getGoodName());
                    }

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

    @PreDestroy
    private void stop() throws InterruptedException {
        managedChannel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        LOGGER.info("Server is terminated!");
    }

}
