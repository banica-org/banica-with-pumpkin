package com.market.banica.calculator.componentTests;

import com.asarkar.grpc.test.GrpcCleanupExtension;
import com.asarkar.grpc.test.Resources;
import com.aurora.Aurora;
import com.aurora.AuroraServiceGrpc;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.market.banica.calculator.CalculatorApplication;
import com.market.banica.calculator.controller.ProductController;
import com.market.banica.calculator.dto.RecipeDTO;
import com.market.banica.calculator.enums.UnitOfMeasure;
import com.market.banica.calculator.model.Product;
import com.market.banica.calculator.service.grpc.AuroraClientSideService;
import com.orderbook.ItemOrderBookResponse;
import com.orderbook.OrderBookLayer;
import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.AdditionalAnswers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@SpringBootTest(classes = CalculatorApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@SpringJUnitConfig
@ExtendWith(GrpcCleanupExtension.class)
@TestPropertySource(locations = "classpath:application-test.properties")
public class CalculatorComponentIT {

    @LocalServerPort
    private int port;

    @Autowired
    private ProductController productController;

    @SpyBean
    @Autowired
    private AuroraClientSideService auroraClientSideService;

    private final AuroraServiceGrpc.AuroraServiceImplBase service =
            mock(AuroraServiceGrpc.AuroraServiceImplBase.class, AdditionalAnswers.delegatesTo(
                    new AuroraServiceGrpc.AuroraServiceImplBase() {
                        @Override
                        public void request(Aurora.AuroraRequest request, StreamObserver<Aurora.AuroraResponse> responseObserver) {
                            String itemName = "product";
                            int price = 2;
                            ItemOrderBookResponse itemOrderBookResponse = ItemOrderBookResponse.newBuilder()
                                    .setItemName(itemName)
                                    .addOrderbookLayers(0, OrderBookLayer.newBuilder()
                                            .setPrice(price)
                                            .build())
                                    .build();

                            responseObserver.onNext(Aurora.AuroraResponse.newBuilder().
                                    setItemOrderBookResponse(itemOrderBookResponse).
                                    build());

                            responseObserver.onCompleted();
                        }
                    }));

    Resources resources;
    AuroraServiceGrpc.AuroraServiceBlockingStub blockingStub;
    private JacksonTester<RecipeDTO> jsonResponseRecipeDto;

    private static String clientId;
    private static String itemName;
    private static int quantity;
    private static RecipeDTO response;
    private static Product product;

    @BeforeAll
    static void setUp() {
        clientId = "1";
        itemName = "product";
        quantity = 2;
        double price = 2;

        product = new Product();
        product.setProductName(itemName);
        product.setUnitOfMeasure(UnitOfMeasure.GRAM);


        response = new RecipeDTO();
        response.setItemName(itemName);
        response.setIngredients(null);
        response.setTotalPrice(BigDecimal.valueOf(price));
    }

    @BeforeEach
    public void SetUp() throws IOException {
        JacksonTester.initFields(this, new ObjectMapper());
        RestAssured.port = port;
        String serverName = InProcessServerBuilder.generateName();

        resources.register(InProcessServerBuilder.forName(serverName).directExecutor()
                        .addService(service).build().start(),
                Duration.of(15000, ChronoUnit.MILLIS));

        ManagedChannel channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
        resources.register(channel, Duration.of(15000, ChronoUnit.MILLIS));
        blockingStub = AuroraServiceGrpc.newBlockingStub(channel);
    }

    @Test
    public void getRecipe_Should_returnIngredientDto_When_thereIsResponse() throws IOException {
        productController.createProduct(Collections.singletonList(product));
        //given
        doReturn(blockingStub).when(auroraClientSideService).getBlockingStub();

        //when & then
        when()
                .get("calculator/" + clientId + "/" + itemName + "/" + quantity)
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .body(is(jsonResponseRecipeDto.write(response).getJson()));
    }

    @AfterEach
    void cleanUp(){
        File data = new File("src/test/resources/backUpRecipeBaseTest.json");
        if(data.length() > 0){
            data.delete();
        }
    }
}
