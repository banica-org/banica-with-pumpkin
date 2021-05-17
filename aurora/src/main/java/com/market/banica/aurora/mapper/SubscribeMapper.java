package com.market.banica.aurora.mapper;

import com.aurora.Aurora;
import com.market.MarketDataRequest;
import com.market.banica.aurora.config.ChannelManager;
import com.market.banica.aurora.config.StubManager;
import com.market.banica.aurora.observer.AuroraObserver;
import com.market.banica.aurora.observer.MarketTickObserver;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.stub.AbstractStub;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;


@Slf4j
@Service
public class SubscribeMapper {

    //TODO: CHANGE VARIABLES NAMES
    private final ChannelManager channelManager;
    private final StubManager stubManager;

    public static final String ORDERBOOK = "orderbook";
    public static final String AURORA = "aurora";
    public static final String MARKET = "market";

    @Autowired
    public SubscribeMapper(ChannelManager channelManager, StubManager stubManager) {

        this.channelManager = channelManager;
        this.stubManager = stubManager;
    }

    public void renderSubscribe(Aurora.AuroraRequest incomingRequest, StreamObserver<Aurora.AuroraResponse> responseObserver) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        String destinationOfMessage = incomingRequest.getTopic().split("/")[0];
        log.info("Accepting render for destionation" + destinationOfMessage);
        List<Map.Entry<String, ManagedChannel>> channelsWithPrefix = channelManager.getAllChannelsContainingPrefix(destinationOfMessage);
        if (channelsWithPrefix.isEmpty()) {
            log.warn("Unsupported message have reached aurora.");
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(incomingRequest.getTopic().split("/")[0] + " channels not available at the moment.")
                    .asException());
            return;
        }

        if (destinationOfMessage.contains(MARKET)) {
            renderMarketMapping(incomingRequest, responseObserver, channelsWithPrefix);

        } else if (destinationOfMessage.contains(AURORA)) {
            renderAuroraMapping(incomingRequest, responseObserver, channelsWithPrefix);
        } else if (destinationOfMessage.contains(ORDERBOOK)) {
            log.warn("Unsupported mapping have reached aurora.");
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription("No provided mapping for odrerbook streaming messages. " + incomingRequest.getTopic())
                    .asException());
        } else {
            log.warn("Unsupported mapping have reached aurora.");
            responseObserver.onError(Status.ABORTED
                    .withDescription("No provided mapping for message " + incomingRequest.getTopic())
                    .asException());
        }
    }


    private void renderAuroraMapping(Aurora.AuroraRequest incomingRequest, StreamObserver<Aurora.AuroraResponse> responseObserver, List<Map.Entry<String, ManagedChannel>> channelsWithPrefix) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        AtomicInteger openStreams = new AtomicInteger(channelsWithPrefix.size());
        String destination = "aurora";

        for (Map.Entry<String, ManagedChannel> channel : channelsWithPrefix) {
            AbstractStub<?> auroraStubMap = stubManager.getStub(channel.getValue(), destination);
            Method auroraSubscribe = auroraStubMap.getClass().getMethod("subscribe", Aurora.AuroraRequest.class, StreamObserver.class);

            auroraSubscribe.invoke(auroraStubMap, incomingRequest, new AuroraObserver(incomingRequest, responseObserver, openStreams));
        }

    }

    private void renderMarketMapping(Aurora.AuroraRequest incomingRequest, StreamObserver<Aurora.AuroraResponse> responseObserver, List<Map.Entry<String, ManagedChannel>> channelsWithPrefix) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        String itemForSubscribing = incomingRequest.getTopic().split("/")[1];
        String destination = "market";
        log.info("Rendering subscribe for item: " + itemForSubscribing);
        AtomicInteger openStreams = new AtomicInteger(channelsWithPrefix.size());

        MarketDataRequest marketDataRequest = MarketDataRequest.newBuilder()
                .setClientId(incomingRequest.getClientId())
                .setGoodName(itemForSubscribing)
                .build();

        for (Map.Entry<String, ManagedChannel> channel : channelsWithPrefix) {
            AbstractStub<?> marketStubMap = stubManager.getStub(channel.getValue(), destination);

            Method marketSubscribeForItem = marketStubMap.getClass().getMethod("subscribeForItem", MarketDataRequest.class, StreamObserver.class);

            marketSubscribeForItem.invoke(marketStubMap, marketDataRequest, new MarketTickObserver(incomingRequest.getClientId(), responseObserver
                    , openStreams, channel.getKey(), marketDataRequest.getGoodName()));
        }

    }
}