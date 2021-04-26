package com.market.banica.order.book.service.grpc;

import com.aurora.Aurora;
import com.aurora.AuroraServiceGrpc;
import com.google.protobuf.InvalidProtocolBufferException;
import com.market.banica.common.channel.ChannelRPCConfig;
import com.market.banica.common.exception.IncorrectResponseException;
import com.market.banica.common.exception.StoppedStreamException;
import com.market.banica.common.exception.TrackingException;
import com.market.banica.order.book.model.ItemMarket;
import com.market.banica.order.book.observer.AuroraStreamObserver;
import com.orderbook.ReconnectionResponse;
import io.grpc.Context;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
@Getter
public class AuroraClient {

    private static final Logger LOGGER = LogManager.getLogger(AuroraClient.class);

    private static final int MAX_RETRY_ATTEMPTS = 1000;
    private static final String MARKET_PREFIX = "market/";

    private final ItemMarket itemMarket;
    private final ManagedChannel managedChannel;
    private final Map<String, Set<Context.CancellableContext>> cancellableStubs;
    private final ExecutorService reconnectionExecutor = Executors.newSingleThreadExecutor();

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

        final Aurora.AuroraRequest request = Aurora.AuroraRequest.newBuilder()
                .setTopic(MARKET_PREFIX + requestedItem)
                .setClientId(clientId)
                .build();

        LOGGER.info("Start gathering product data.");

        Context.CancellableContext withCancellation = Context.current().withCancellation();
        cancellableStubs.put(requestedItem, Collections.synchronizedSet(
                new HashSet<>(Collections.singletonList(withCancellation))));
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

        Set<Context.CancellableContext> cancelledStub = cancellableStubs.remove(requestedItem);
        cancelledStub.forEach(cancellableContext -> cancellableContext
                .cancel(new StoppedStreamException("Stopped tracking stream for: " + requestedItem)));
        itemMarket.removeUntrackedItem(requestedItem);
    }

    public void reconnectToMarket(Aurora.AuroraResponse response) {

        reconnectionExecutor.execute(() -> {
            ReconnectionResponse reconnectionResponse;

            try {
                reconnectionResponse = response.getMessage().unpack(ReconnectionResponse.class);
            } catch (InvalidProtocolBufferException e) {
                throw new IncorrectResponseException("Incorrect response! Response must be from ReconnectionResponse type.");
            }

            String itemName = reconnectionResponse.getItemName();
            String marketDestination = reconnectionResponse.getDestination();
            String clientId = reconnectionResponse.getClientId();

            itemMarket.zeroingMarketProductsFromMarket(marketDestination, itemName);

            final Aurora.AuroraRequest request = Aurora.AuroraRequest.newBuilder()
                    .setTopic(marketDestination + "/" + itemName)
                    .setClientId(clientId)
                    .build();

            Context.CancellableContext withCancellation = Context.current().withCancellation();
            cancellableStubs.get(itemName).add(withCancellation);

            try {
                withCancellation.run(() -> startMarketStream(request));
            } catch (Exception e) {
                LOGGER.error("Tracking for {} has suddenly stopped due to: {}", itemName, e.getMessage());
            }
        });

    }

    private void startMarketStream(Aurora.AuroraRequest request) {
        final AuroraServiceGrpc.AuroraServiceStub asynchronousStub = getAsynchronousStub();

        asynchronousStub.subscribe(request, new AuroraStreamObserver(itemMarket, this));
    }

    public AuroraServiceGrpc.AuroraServiceStub getAsynchronousStub() {
        return AuroraServiceGrpc.newStub(managedChannel);
    }

    @PreDestroy
    private void stop() throws InterruptedException {
        managedChannel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        LOGGER.info("Server is terminated!");
    }

}
