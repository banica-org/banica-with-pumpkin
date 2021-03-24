package com.market.banica.calculator.componentTests;

import com.aurora.Aurora;
import com.aurora.AuroraServiceGrpc;
import com.market.banica.calculator.CalculatorApplication;
import com.market.banica.calculator.dto.RecipeDTO;
import com.market.banica.calculator.service.grpc.AuroraClientSideService;
import com.orderbook.ItemOrderBookResponse;
import com.orderbook.OrderBookLayer;
import io.grpc.Status;
import io.restassured.RestAssured;
import org.grpcmock.springboot.AutoConfigureGrpcMock;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.math.BigDecimal;

import static io.restassured.RestAssured.when;
import static org.grpcmock.GrpcMock.calledMethod;
import static org.grpcmock.GrpcMock.stubFor;
import static org.grpcmock.GrpcMock.unaryMethod;
import static org.grpcmock.GrpcMock.verifyThat;
import static org.hamcrest.Matchers.is;

@SpringJUnitConfig
@SpringBootTest(classes = CalculatorApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureGrpcMock
public class AuroraClientSideServiceComponentTest {

    @LocalServerPort
    private int grpcMockPort;

    private static String clientId;
    private static String itemName;
    private static String quantity;
    private static RecipeDTO response;
    private static ItemOrderBookResponse itemOrderBookResponse;
    private static Aurora.AuroraRequest expectedRequest;

    @BeforeEach
    void setupChannel() {
        RestAssured.port = grpcMockPort;
    }

    @BeforeAll
    static void setUp() {
        clientId = "1";
        itemName = "product";
        quantity = "1.9";
        double price = 2.9;

        response = new RecipeDTO();
        response.setItemName(itemName);
        response.setIngredients(null);
        response.setTotalPrice(BigDecimal.valueOf(price));

        itemOrderBookResponse = ItemOrderBookResponse.newBuilder()
                .setItemName(itemName)
                .addOrderbookLayers(0, OrderBookLayer.newBuilder()
                        .setPrice(price)
                        .build())
                .build();

        expectedRequest = Aurora.AuroraRequest.newBuilder()
                .setClientId(clientId)
                .setTopic(AuroraClientSideService.ORDERBOOK_TOPIC_PREFIX + itemName)
                .build();
    }

    @Test
    public void getRecipe_Should_returnIngredientDto_When_thereIsResponse() {
        //given
        Aurora.AuroraRequest expectedRequest = Aurora.AuroraRequest.newBuilder()
                .setClientId(clientId)
                .setTopic(AuroraClientSideService.ORDERBOOK_TOPIC_PREFIX + itemName)
                .build();

        stubFor(unaryMethod(AuroraServiceGrpc.getRequestMethod())
                .willReturn(Aurora.AuroraResponse.newBuilder()
                        .setItemOrderBookResponse(itemOrderBookResponse)
                        .build()));

        //when & then
        when()
                .get("calculator/" + clientId + "/" + itemName + "/" + quantity)
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .body(is(response));

        verifyThat(calledMethod(AuroraServiceGrpc.getRequestMethod())
                .withRequest(expectedRequest));
    }

    @Test
    void getRecipe_Should_returnError_When_thereIsErrorResponse() {
        //given
        stubFor(unaryMethod(AuroraServiceGrpc.getRequestMethod())
                .willReturn(Status.INVALID_ARGUMENT.withDescription("some error")));

        //when & then
        when()
                .get("calculator/" + clientId + "/" + itemName + "/" + quantity)
                .then()
                .assertThat()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());

        verifyThat(calledMethod(AuroraServiceGrpc.getRequestMethod())
                .withRequest(expectedRequest));
    }
}
