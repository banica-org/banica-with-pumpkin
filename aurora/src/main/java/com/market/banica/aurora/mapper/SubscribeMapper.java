package com.market.banica.aurora.mapper;

import com.aurora.Aurora;
import com.aurora.AuroraServiceGrpc;
import com.market.MarketDataRequest;
import com.market.MarketServiceGrpc;
import com.market.banica.aurora.config.ChannelManager;
import com.market.banica.aurora.config.StubManager;
import com.market.banica.aurora.observer.AuroraObserver;
import com.market.banica.aurora.observer.MarketTickObserver;
import io.grpc.ManagedChannel;
import io.grpc.Status;
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
        //List<ManagedChannel> channelsWithPrefix = channelManager.getAllChannelsContainingPrefix(destinationOfMessage);
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

        for (Map.Entry<String, ManagedChannel> channel : channelsWithPrefix) {
            Map.Entry<AuroraServiceGrpc.AuroraServiceStub, Method[]> auroraStubMap = stubManager.getAuroraStub(channel.getValue());
            Method[] auroraBlockingStubMethods = auroraStubMap.getValue();
            Method auroraSubscribe = auroraBlockingStubMethods.getClass().getMethod("subscribe", Aurora.AuroraRequest.class, AuroraObserver.class);

            auroraSubscribe.invoke(auroraStubMap.getKey(), incomingRequest, new AuroraObserver(incomingRequest, responseObserver, openStreams));
        }

    }

    private void renderMarketMapping(Aurora.AuroraRequest incomingRequest, StreamObserver<Aurora.AuroraResponse> responseObserver, List<Map.Entry<String, ManagedChannel>> channelsWithPrefix) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        String itemForSubscribing = incomingRequest.getTopic().split("/")[1];
        AtomicInteger openStreams = new AtomicInteger(channelsWithPrefix.size());

        MarketDataRequest marketDataRequest = MarketDataRequest.newBuilder()
                .setClientId(incomingRequest.getClientId())
                .setGoodName(itemForSubscribing)
                .build();

        for (Map.Entry<String, ManagedChannel> channel : channelsWithPrefix) {
            Map.Entry<MarketServiceGrpc.MarketServiceStub, Method[]> marketStubMap = stubManager.getMarketStub(channel.getValue());
            Method[] marketStubMethods = marketStubMap.getValue();
            Method marketSubscribeForItem = marketStubMethods.getClass().getMethod("subscribeForItem", MarketDataRequest.class, MarketTickObserver.class);

            marketSubscribeForItem.invoke(marketStubMap.getKey(), marketDataRequest, new MarketTickObserver(incomingRequest.getClientId(), responseObserver
                    , openStreams, channel.getKey(), marketDataRequest.getGoodName()));
        }

    }
}