package com.market.banica.aurora.service;

import com.aurora.Aurora;
import com.market.TickResponse;
import com.market.banica.aurora.channel.MarketChannelManager;
import com.market.banica.aurora.client.MarketClient;
import com.market.banica.aurora.observers.MarketTickResponseObserver;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;

import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class AuroraSubscriptionManagerTest {
    private static final Aurora.AuroraRequest AURORA_REQUEST = Aurora.AuroraRequest.newBuilder().setClientId("clientId").setTopic("*/Eggs").build();


    @Mock
    private StreamObserver<Aurora.AuroraResponse> auroraResponseStreamObserver;

    @Mock
    private StreamObserver<TickResponse> tickResponseObserver;

    @Mock
    private MarketTickResponseObserver responseObserver;

    private MarketChannelManager marketChannelManager;

    private MarketClient marketClient;

    Map<String, ManagedChannel> channels = new HashMap<>();

    private AuroraSubscriptionManager auroraSubscriptionManager;

    @Before
    public void setUp() {

        System.out.println();
        marketChannelManager = Mockito.mock(MarketChannelManager.class);
        marketClient = Mockito.mock(MarketClient.class);

        this.auroraSubscriptionManager = new AuroraSubscriptionManager(marketChannelManager, marketClient);
        this.tickResponseObserver = Mockito.spy(new MarketTickResponseObserver(auroraSubscriptionManager));

        channels.put("Asia", ManagedChannelBuilder.forAddress("any", 0).build());
        channels.put("America", ManagedChannelBuilder.forAddress("any", 0).build());
        channels.put("Europe", ManagedChannelBuilder.forAddress("any", 0).build());
    }

    @Test
    public void subscribe() {

    }
}