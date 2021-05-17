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

import static org.junit.jupiter.api.Assertions.*;

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
        Channel actualChannel = stubManager.getAuroraStub(DUMMY_MANAGED_CHANNEL).getChannel();
        Channel expectedChannel = AuroraServiceGrpc.newStub(DUMMY_MANAGED_CHANNEL).getChannel();
        assertEquals(expectedChannel,actualChannel);
    }

    @Test
    void getOrderbookStubCreatesAndReturnsOrderBookStubForSpecifiedChannel() {
        Channel actualChannel = stubManager.getOrderbookBlockingStub(DUMMY_MANAGED_CHANNEL).getChannel();
        Channel expectedChannel = OrderBookServiceGrpc.newStub(DUMMY_MANAGED_CHANNEL).getChannel();
        assertEquals(expectedChannel,actualChannel);
    }

    @Test
    void getOrderbookBlockingStubCreatesAndReturnsOrderBookBlockingStubForSpecifiedChannel() {
        Channel actualChannel = stubManager.getOrderbookBlockingStub(DUMMY_MANAGED_CHANNEL).getChannel();
        Channel expectedChannel = OrderBookServiceGrpc.newBlockingStub((DUMMY_MANAGED_CHANNEL)).getChannel();
        assertEquals(expectedChannel,actualChannel);
    }

    @Test
    void getMarketStubCreatesAndReturnsMarketStubForSpecifiedChannel() {
        Channel actualChannel = stubManager.getMarketStub(DUMMY_MANAGED_CHANNEL).getChannel();
        Channel expectedChannel = MarketServiceGrpc.newStub(DUMMY_MANAGED_CHANNEL).getChannel();
        assertEquals(expectedChannel,actualChannel);
    }
}