package com.market.banica.aurora.config;

import com.aurora.AuroraServiceGrpc;
import com.market.MarketServiceGrpc;
import com.orderbook.OrderBookServiceGrpc;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class StubManagerTest {

    private static final ManagedChannel DUMMY_MANAGED_CHANNEL = ManagedChannelBuilder
            .forAddress("localhost", 1010)
            .usePlaintext()
            .build();

    @Spy
    private StubManager stubManager;

    @Test
    void getAuroraStubCreatesAndReturnsAuroraStubForSpecifiedChannel() {
        Channel actualChannel = stubManager.getStub(DUMMY_MANAGED_CHANNEL, "aurora").getChannel();
        Channel expectedChannel = AuroraServiceGrpc.newStub(DUMMY_MANAGED_CHANNEL).getChannel();
        assertEquals(expectedChannel, actualChannel);
    }

    @Test
    void getAuroraBlockingStubCreatesAndReturnsAuroraStubForSpecifiedChannel() {
        Channel actualChannel = stubManager.getBlockingStub(DUMMY_MANAGED_CHANNEL, "aurora").getChannel();
        Channel expectedChannel = AuroraServiceGrpc.newBlockingStub(DUMMY_MANAGED_CHANNEL).getChannel();
        assertEquals(expectedChannel, actualChannel);
    }

    @Test
    void getOrderbookStubCreatesAndReturnsOrderBookStubForSpecifiedChannel() {
        Channel actualChannel = stubManager.getStub(DUMMY_MANAGED_CHANNEL, "orderbook").getChannel();
        Channel expectedChannel = OrderBookServiceGrpc.newStub(DUMMY_MANAGED_CHANNEL).getChannel();
        assertEquals(expectedChannel, actualChannel);
    }

    @Test
    void getOrderbookBlockingStubCreatesAndReturnsOrderBookBlockingStubForSpecifiedChannel() {
        Channel actualChannel = stubManager.getBlockingStub(DUMMY_MANAGED_CHANNEL, "orderbook").getChannel();
        Channel expectedChannel = OrderBookServiceGrpc.newBlockingStub((DUMMY_MANAGED_CHANNEL)).getChannel();
        assertEquals(expectedChannel, actualChannel);
    }

    @Test
    void getMarketStubCreatesAndReturnsMarketStubForSpecifiedChannel() {
        Channel actualChannel = stubManager.getStub(DUMMY_MANAGED_CHANNEL, "market").getChannel();
        Channel expectedChannel = MarketServiceGrpc.newStub(DUMMY_MANAGED_CHANNEL).getChannel();
        assertEquals(expectedChannel, actualChannel);
    }

    @Test
    void getMarketBlockingStubCreatesAndReturnsMarketStubForSpecifiedChannel() {
        Channel actualChannel = stubManager.getBlockingStub(DUMMY_MANAGED_CHANNEL, "market").getChannel();
        Channel expectedChannel = MarketServiceGrpc.newBlockingStub(DUMMY_MANAGED_CHANNEL).getChannel();
        assertEquals(expectedChannel, actualChannel);
    }
}