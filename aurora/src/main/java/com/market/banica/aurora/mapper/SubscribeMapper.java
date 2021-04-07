package com.market.banica.aurora.mapper;

import com.aurora.Aurora;
import com.market.MarketDataRequest;
import com.market.banica.aurora.config.ChannelManager;
import com.market.banica.aurora.config.StubManager;
import com.market.banica.aurora.observer.AuroraObserver;
import com.market.banica.aurora.observer.MarketTickObserver;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


@Service
public class SubscribeMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubscribeMapper.class);


    private final ChannelManager channelManager;
    private final StubManager stubManager;

    @Autowired
    public SubscribeMapper(ChannelManager channelManager, StubManager stubManager) {

        this.channelManager = channelManager;
        this.stubManager = stubManager;
    }

    public void renderSubscribe(Aurora.AuroraRequest incomingRequest, StreamObserver<Aurora.AuroraResponse> responseObserver) {
        String destinationOfMessage = incomingRequest.getTopic().split("/")[0];
        List<ManagedChannel> channelsWithPrefix = channelManager.getAllChannelsContainingPrefix(destinationOfMessage);

        if (channelsWithPrefix.isEmpty()) {
            LOGGER.warn("Unsupported message have reached aurora.");
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(incomingRequest.getTopic().split("/")[0] + " channels not available at the moment.")
                    .asException());
            return;
        }

        if (destinationOfMessage.contains("market")) {
            String itemForSubscribing = incomingRequest.getTopic().split("/")[1];
            AtomicInteger openStreams = new AtomicInteger(channelsWithPrefix.size());

            MarketDataRequest marketDataRequest = MarketDataRequest.newBuilder()
                    .setClientId(incomingRequest.getClientId())
                    .setGoodName(itemForSubscribing)
                    .build();

            channelsWithPrefix.forEach(channel -> stubManager.getMarketStub(channel)
                    .subscribeForItem(marketDataRequest, new MarketTickObserver(incomingRequest.getClientId(), responseObserver, openStreams)));

        } else if (destinationOfMessage.contains("aurora")) {
            AtomicInteger openStreams = new AtomicInteger(channelsWithPrefix.size());

            channelsWithPrefix.forEach(channel -> this.stubManager.getAuroraStub(channel)
                    .subscribe(incomingRequest, new AuroraObserver(incomingRequest, responseObserver, openStreams)));
        } else {
            LOGGER.warn("Unsupported mapping have reached aurora.");
            responseObserver.onError(Status.ABORTED
                    .withDescription("No provided mapping for message " + incomingRequest.getTopic())
                    .asException());
        }
    }
}
