package com.market.banica.calculator.componentTests;

import com.asarkar.grpc.test.GrpcCleanupExtension;
import com.asarkar.grpc.test.Resources;
import com.aurora.AuroraServiceGrpc;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.market.banica.calculator.CalculatorApplication;
import com.market.banica.calculator.componentTests.configuration.TestConfigurationIT;
import com.market.banica.calculator.controller.ProductController;
import com.market.banica.calculator.dto.RecipeDTO;
import com.market.banica.calculator.enums.UnitOfMeasure;
import com.market.banica.calculator.model.Product;
import com.market.banica.calculator.service.grpc.AuroraClientSideService;
import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
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

@SpringBootTest(classes = CalculatorApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@SpringJUnitConfig
@ExtendWith({GrpcCleanupExtension.class})
@ActiveProfiles("testIT")
public class CalculatorComponentIT {

    @LocalServerPort
    private int port;

    @Autowired
    private ProductController productController;

    @Autowired
    private TestConfigurationIT testConfigurationIT;

    @SpyBean
    @Autowired
    private AuroraClientSideService auroraClientSideService;

    @Value(value = "${client.id}")
    private String clientId;

    @Value(value = "${product.name}")
    private String productName;

    @Value(value = "${product.quantity}")
    private int productQuantity;

    @Value(value = "${product.price}")
    private double price;

    @Value(value = "${resource.timeout}")
    private int timeout;

    @Value(value = "${database.backup.url}")
    private String databaseBackupUrl;

    private RecipeDTO response;
    private Product product;

    private Resources resources;
    private Duration duration;

    private AuroraServiceGrpc.AuroraServiceBlockingStub blockingStub;
    private JacksonTester<RecipeDTO> jsonResponseRecipeDto;

    private String calculatorControllerGetRecipeUrl;
    private String productControllerCreateProductUrl;

    @BeforeEach
    public void SetUp() throws IOException {
        JacksonTester.initFields(this, new ObjectMapper());
        RestAssured.port = port;

        calculatorControllerGetRecipeUrl =
                "calculator/" + clientId + "/" + productName + "/" + productQuantity;
        productControllerCreateProductUrl = "product";

        product = new Product();
        product.setProductName(productName);
        product.setUnitOfMeasure(UnitOfMeasure.GRAM);

        response = new RecipeDTO();
        response.setItemName(productName);
        response.setIngredients(null);
        response.setTotalPrice(BigDecimal.valueOf(price));

        duration = Duration.of(timeout, ChronoUnit.MILLIS);

        resources.register(testConfigurationIT.getChannel(), duration);

        blockingStub = testConfigurationIT.createBlockingStub();
    }

    @Test
    public void getRecipe_Should_returnRecipeDto_When_thereIsResponse() throws IOException {
        //given
        productController.createProduct(Collections.singletonList(product));
        resources.register(testConfigurationIT.startInProcessService(), duration);
        doReturn(blockingStub).when(auroraClientSideService).getBlockingStub();

        //when & then
        when()
                .get(calculatorControllerGetRecipeUrl)
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .body(is(jsonResponseRecipeDto.write(response).getJson()));
    }

    @Test
    public void getRecipe_Should_returnError_When_thereIsNoResponse() throws IOException {
        //given
        productController.createProduct(Collections.singletonList(product));
        resources.register(testConfigurationIT.startInProcessServiceWithEmptyService(), duration);
        doReturn(blockingStub).when(auroraClientSideService).getBlockingStub();

        //when & then
        when()
                .get(calculatorControllerGetRecipeUrl)
                .then()
                .assertThat()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    @Test
    public void createProduct_Should_returnProduct_When_thereIsResponse() throws IOException {
        //given
        resources.register(testConfigurationIT.startInProcessService(), duration);
        doReturn(blockingStub).when(auroraClientSideService).getBlockingStub();

        //when & then
        when()
                .get(calculatorControllerGetRecipeUrl)
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .body(is(jsonResponseRecipeDto.write(response).getJson()));
    }

    @AfterEach
    void cleanUp() {
        File data = new File(databaseBackupUrl);
        if (data.length() > 0) {
            data.delete();
        }
    }
}
