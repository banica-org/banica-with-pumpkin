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
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.Rule;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;


@SpringJUnitConfig
@SpringBootTest(classes = MarketGeneratorApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("testMarketIT")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
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

        marketName = InProcessServerBuilder.generateName();
        marketChannel = InProcessChannelBuilder
                .forName(marketName)
                .directExecutor()
                .build();

    }

    @AfterAll
    public static void cleanUp() throws IOException {
        ApplicationDirectoryUtil.getConfigFile(marketStateName).delete();
        ApplicationDirectoryUtil.getConfigFile(marketSnapshotName).delete();
        ApplicationDirectoryUtil.getConfigFile(marketPropertiesName).delete();
    }

    @Test
    public void subscribeForItem_Should_Work() throws InterruptedException, IOException {

        //Arrange
        createFakeServerMarket();
        grpcCleanup.register(marketChannel);

        MarketDataRequest request = createRequest();

        List<TickResponse> expectedResponses = new ArrayList<>();
        addExpectedResponses(expectedResponses);

        ArrayList<TickResponse> receivedResponses = new ArrayList<>();

        CountDownLatch latch = new CountDownLatch(5);

        //Act
        createStub(request, receivedResponses, latch);

        latch.await();

        //Assert
        assertEquals(expectedResponses, receivedResponses);

    }

    @Test
    public void subscribeForItem_Should_SendLessResponses_WhenServiceSendsError() throws InterruptedException, IOException {

        //Arrange
        createFakeErrorServerMarket();
        grpcCleanup.register(marketChannel);

        MarketDataRequest request = createRequest();

        List<TickResponse> expectedResponses = new ArrayList<>();
        addExpectedResponses(expectedResponses);

        ArrayList<TickResponse> receivedResponses = new ArrayList<>();

        CountDownLatch latch = new CountDownLatch(3);

        //Act
        createStub(request, receivedResponses, latch);

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
                    public void subscribeForItem(MarketDataRequest request, StreamObserver<TickResponse> responseObserver) {
                        marketService.subscribeForItem(request, responseObserver);
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
                    public void subscribeForItem(MarketDataRequest request, StreamObserver<TickResponse> responseObserver) {
                        for (int i = 0; i < 5; i++) {
                            if (i == 3) {
                                responseObserver.onError(Status.ABORTED
                                        .withDescription("Aborting subscribe.")
                                        .asException());
                                break;
                            }
                            MarketTick marketTick = new MarketTick(GOOD, 10, 1.0, new Date().getTime());
                            TickResponse response = TickResponse.newBuilder()
                                    .setOrigin(MarketTick.getOrigin())
                                    .setGoodName(marketTick.getGood())
                                    .setQuantity(marketTick.getQuantity())
                                    .setPrice(marketTick.getPrice())
                                    .setTimestamp(marketTick.getTimestamp())
                                    .build();
                            responseObserver.onNext(response);
                        }
                    }
                })
                .build()
                .start());
    }

    private MarketDataRequest createRequest() {
        return MarketDataRequest.newBuilder()
                .setClientId("integration test")
                .setGoodName(GOOD)
                .build();
    }

    private void addExpectedResponses(List<TickResponse> expectedResponses) {
        for (int i = 0; i < 5; i++) {
            MarketTick.setOrigin("AMERICA");
            MarketTick marketTick = new MarketTick(GOOD, 10, 1.0, new Date().getTime());
            marketState.addTickToMarketSnapshot(marketTick);

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

    private void createStub(MarketDataRequest request, ArrayList<TickResponse> receivedResponses, CountDownLatch latch){
        MarketServiceGrpc.newStub(marketChannel).subscribeForItem(request, new StreamObserver<TickResponse>() {
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
