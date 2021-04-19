package com.market.banica.order.book.service.grpc;

import com.aurora.Aurora;
import com.aurora.AuroraServiceGrpc;
import com.market.banica.common.channel.ChannelRPCConfig;
import com.market.banica.order.book.exception.StoppedStreamException;
import com.market.banica.order.book.exception.TrackingException;
import com.market.banica.order.book.model.ItemMarket;
import com.market.banica.order.book.observer.AuroraStreamObserver;
import io.grpc.Context;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
@Getter
public class AuroraClient {
    private final ItemMarket itemMarket;
    private final ManagedChannel managedChannel;
    private final Map<String, Context.CancellableContext> cancellableStubs;

    private static final int MAX_RETRY_ATTEMPTS = 1000;
    private static final String MARKET_PREFIX = "market/";
    private static final Logger LOGGER = LogManager.getLogger(AuroraClient.class);

    @Autowired
    AuroraClient(ItemMarket itemMarket,
                 @Value("${aurora.server.host}") final String host,
                 @Value("${aurora.server.port}") final int port) {

        managedChannel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .defaultServiceConfig(ChannelRPCConfig.getInstance().getServiceConfig())
                .enableRetry()
                .maxRetryAttempts(MAX_RETRY_ATTEMPTS)
                .build();

        this.itemMarket = itemMarket;
        this.cancellableStubs = new ConcurrentHashMap<>();

    }

    public void startSubscription(String requestedItem, String clientId) throws TrackingException {

        if (cancellableStubs.containsKey(requestedItem)) {
            throw new TrackingException("Item is already being tracked!");
        }

        final Aurora.AuroraRequest request = buildAuroraRequest(requestedItem, clientId);

        LOGGER.info("Start gathering product data.");

        Context.CancellableContext withCancellation = Context.current().withCancellation();
        cancellableStubs.put(requestedItem, withCancellation);
        itemMarket.addTrackedItem(requestedItem);
        try {
            withCancellation.run(() -> startMarketStream(request));
        } catch (Exception e) {
            LOGGER.error("Tracking for {} has suddenly stopped due to: {}", requestedItem, e.getMessage());
        }
    }

    public void stopSubscription(String requestedItem, String clientId) throws TrackingException {

        if (!cancellableStubs.containsKey(requestedItem)) {
            throw new TrackingException("Item is not being tracked!");
        }

        Context.CancellableContext cancelledStub = cancellableStubs.remove(requestedItem);
        cancelledStub.cancel(new StoppedStreamException("Stopped tracking stream for: " + requestedItem));
        itemMarket.removeUntrackedItem(requestedItem);
        itemMarket.removeItemFromFileBackUp(requestedItem);
    }

    public AuroraServiceGrpc.AuroraServiceStub getAsynchronousStub() {
        return AuroraServiceGrpc.newStub(managedChannel);
    }

    @PostConstruct
    private void subscribeOnCreation() {
        Set<String> subscribedItems = this.itemMarket.getSubscribedItems();
        for (String itemName : subscribedItems) {
            System.out.println("curr item name -> " + itemName);
            try {
                this.startSubscription(itemName, "calculator");
            } catch (TrackingException e) {
                LOGGER.error(e.getMessage());
            }
        }
    }

    @PreDestroy
    private void stop() throws InterruptedException {
        managedChannel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        LOGGER.info("Server is terminated!");
    }

    private void startMarketStream(Aurora.AuroraRequest request) {
        final AuroraServiceGrpc.AuroraServiceStub asynchronousStub = getAsynchronousStub();

        asynchronousStub.subscribe(request, new AuroraStreamObserver(itemMarket));
        this.itemMarket.persistItemInFileBackUp(request.getTopic().split(MARKET_PREFIX)[1]);
    }

    private Aurora.AuroraRequest buildAuroraRequest(String requestedItem, String clientId) {
        return Aurora.AuroraRequest.newBuilder()
                .setTopic(MARKET_PREFIX + requestedItem)
                .setClientId(clientId)
                .build();
    }
}
