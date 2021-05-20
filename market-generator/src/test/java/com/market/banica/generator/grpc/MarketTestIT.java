package com.market.banica.generator.grpc;

import com.market.MarketDataRequest;
import com.market.MarketServiceGrpc;
import com.market.TickResponse;
import com.market.banica.common.util.ApplicationDirectoryUtil;
import com.market.banica.generator.MarketGeneratorApplication;
import com.market.banica.generator.model.MarketTick;
import com.market.banica.generator.service.MarketState;
import com.market.banica.generator.service.grpc.MarketService;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.Rule;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;


@SpringJUnitConfig
@SpringBootTest(classes = MarketGeneratorApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("testMarketIT")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class MarketTestIT {

    @Rule
    public GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    @Autowired
    MarketService marketService;

    @Autowired
    MarketState marketState;


    private static final String GOOD = "eggs";

    private String marketName;
    private ManagedChannel marketChannel;

    private static String marketStateName;
    private static String marketSnapshotName;
    private static String marketPropertiesName;


    @BeforeAll
    public static void getFilePath(@Value("${tick.market.state.file.name}") String marketStateFileName,
                                   @Value("${tick.market.snapshot.file.name}") String marketSnapshotFileName,
                                   @Value("${market.properties.file.name}") String marketPropertiesFileName) {
        marketStateName = marketStateFileName;
        marketSnapshotName = marketSnapshotFileName;
        marketPropertiesName = marketPropertiesFileName;

    }

    @BeforeEach
    public void setup() {

        ReflectionTestUtils.setField(marketState, "marketState", new ConcurrentHashMap<String, Set<MarketTick>>());
        ReflectionTestUtils.setField(marketState, "marketSnapshot", new LinkedBlockingQueue<MarketTick>());

        marketName = InProcessServerBuilder.generateName();
        marketChannel = InProcessChannelBuilder
                .forName(marketName)
                .directExecutor()
                .build();

    }

    @AfterEach
    void tearDown() throws IOException {

        ApplicationDirectoryUtil.getConfigFile(marketSnapshotName).delete();
        assert ApplicationDirectoryUtil.getConfigFile(marketStateName).delete();
        assert ApplicationDirectoryUtil.getConfigFile(marketPropertiesName).delete();

    }

    @AfterAll
    public static void cleanUp() throws IOException {

        assert ApplicationDirectoryUtil.getConfigFile(marketStateName).delete();
        assert ApplicationDirectoryUtil.getConfigFile(marketSnapshotName).delete();
        assert ApplicationDirectoryUtil.getConfigFile(marketPropertiesName).delete();

    }

    @Test
    public void subscribeForItem_Should_SendResponses() throws InterruptedException, IOException {

        //Arrange
        createFakeServerMarket();
        grpcCleanup.register(marketChannel);

        List<TickResponse> expectedResponses = new ArrayList<>();
        addExpectedResponses(expectedResponses);

        ArrayList<TickResponse> receivedResponses = new ArrayList<>();

        CountDownLatch latch = new CountDownLatch(5);

        //Act
        createStub(receivedResponses, latch);

        latch.await();

        //Assert
        assertEquals(expectedResponses, receivedResponses);

    }

    @Test
    public void subscribeForItem_Should_SendLessResponses_WhenServiceSendsError() throws InterruptedException, IOException {

        //Arrange
        createFakeErrorServerMarket();
        grpcCleanup.register(marketChannel);

        List<TickResponse> expectedResponses = new ArrayList<>();
        addExpectedResponses(expectedResponses);

        ArrayList<TickResponse> receivedResponses = new ArrayList<>();

        CountDownLatch latch = new CountDownLatch(3);

        //Act
        createStub(receivedResponses, latch);

        latch.await();

        //Assert
        assertNotEquals(expectedResponses, receivedResponses);

    }

    private void createFakeServerMarket() throws IOException {
        grpcCleanup.register(InProcessServerBuilder
                .forName(marketName)
                .directExecutor()
                .addService(new MarketServiceGrpc.MarketServiceImplBase() {
                    @Override
                    public StreamObserver<MarketDataRequest> subscribeForItem(StreamObserver<TickResponse> responseObserver) {
                        for (int i = 0; i < 5; i++) {
                            MarketTick marketTick = new MarketTick(GOOD, 10, 1.0, i);
                            TickResponse response = TickResponse.newBuilder()
                                    .setOrigin(MarketTick.getOrigin())
                                    .setGoodName(marketTick.getGood())
                                    .setQuantity(marketTick.getQuantity())
                                    .setPrice(marketTick.getPrice())
                                    .setTimestamp(marketTick.getTimestamp())
                                    .build();
                            responseObserver.onNext(response);
                        }
                        return fakeStreamObserver();
                    }
                })
                .build()
                .start());
    }

    private void createFakeErrorServerMarket() throws IOException {
        grpcCleanup.register(InProcessServerBuilder
                .forName(marketName)
                .directExecutor()
                .addService(new MarketServiceGrpc.MarketServiceImplBase() {
                    @Override
                    public StreamObserver<MarketDataRequest> subscribeForItem(StreamObserver<TickResponse> responseObserver) {
                        for (int i = 0; i < 5; i++) {
                            if (i == 3) {
                                responseObserver.onError(Status.ABORTED
                                        .withDescription("Aborting subscribe.")
                                        .asException());
                                break;
                            }
                            MarketTick marketTick = new MarketTick(GOOD, 10, 1.0, i);
                            TickResponse response = TickResponse.newBuilder()
                                    .setOrigin(MarketTick.getOrigin())
                                    .setGoodName(marketTick.getGood())
                                    .setQuantity(marketTick.getQuantity())
                                    .setPrice(marketTick.getPrice())
                                    .setTimestamp(marketTick.getTimestamp())
                                    .build();
                            responseObserver.onNext(response);
                        }
                        return fakeStreamObserver();
                    }
                })
                .build()
                .start());
    }

    private void addExpectedResponses(List<TickResponse> expectedResponses) {
        for (int i = 0; i < 5; i++) {
            MarketTick.setOrigin("AMERICA");
            MarketTick marketTick = new MarketTick(GOOD, 10, 1.0, i);
            marketState.addTickToMarket(marketTick);

            TickResponse response = TickResponse.newBuilder()
                    .setOrigin(MarketTick.getOrigin())
                    .setGoodName(marketTick.getGood())
                    .setQuantity(marketTick.getQuantity())
                    .setPrice(marketTick.getPrice())
                    .setTimestamp(marketTick.getTimestamp())
                    .build();

            expectedResponses.add(response);
        }
    }

    private StreamObserver<MarketDataRequest> fakeStreamObserver() {
        return new StreamObserver<MarketDataRequest>() {
            @Override
            public void onNext(MarketDataRequest marketDataRequest) {

            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onCompleted() {

            }
        };
    }

    private void createStub(ArrayList<TickResponse> receivedResponses, CountDownLatch latch) {
        MarketServiceGrpc.newStub(marketChannel).subscribeForItem(new ClientCallStreamObserver<TickResponse>() {
            @Override
            public void cancel(String s, Throwable throwable) {

            }

            @Override
            public boolean isReady() {
                return false;
            }

            @Override
            public void setOnReadyHandler(Runnable runnable) {

            }

            @Override
            public void disableAutoInboundFlowControl() {

            }

            @Override
            public void request(int i) {

            }

            @Override
            public void setMessageCompression(boolean b) {

            }

            @Override
            public void onNext(TickResponse tickResponse) {
                receivedResponses.add(tickResponse);
                latch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                latch.countDown();
            }

            @Override
            public void onCompleted() {
                latch.countDown();
            }
        });
    }
}
