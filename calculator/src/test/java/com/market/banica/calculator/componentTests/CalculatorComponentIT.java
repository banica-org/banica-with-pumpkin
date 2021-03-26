package com.market.banica.calculator.componentTests;

import com.asarkar.grpc.test.GrpcCleanupExtension;
import com.asarkar.grpc.test.Resources;
import com.aurora.AuroraServiceGrpc;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.market.banica.calculator.CalculatorApplication;
import com.market.banica.calculator.componentTests.configuration.TestConfigurationIT;
import com.market.banica.calculator.controller.ProductController;
import com.market.banica.calculator.data.contract.ProductBase;
import com.market.banica.calculator.dto.RecipeDTO;
import com.market.banica.calculator.enums.UnitOfMeasure;
import com.market.banica.calculator.model.Product;
import com.market.banica.calculator.service.grpc.AuroraClientSideService;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.doReturn;

@SpringBootTest(classes = CalculatorApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith({GrpcCleanupExtension.class, MockitoExtension.class})
@ActiveProfiles("testIT")
public class CalculatorComponentIT {

    @LocalServerPort
    private int port;

    @Autowired
    private ProductController productController;

    @Autowired
    private ProductBase productBase;

    @Autowired
    private TestConfigurationIT testConfigurationIT;

    @SpyBean
    @Autowired
    private AuroraClientSideService auroraClientSideService;

    @Value(value = "${client-id}")
    private String clientId;

    @Value(value = "${product-name}")
    private String productName;

    @Value(value = "${product-quantity}")
    private int productQuantity;

    @Value(value = "${product-price}")
    private double price;

    @Value(value = "${resource-timeout}")
    private int timeout;

    @Value(value = "${database-backup-url}")
    private String databaseBackupUrl;

    private RecipeDTO response;
    private Product product;

    private Resources resources;
    private Duration duration;
    private AuroraServiceGrpc.AuroraServiceBlockingStub blockingStub;

    private JacksonTester<RecipeDTO> jsonResponseRecipeDto;
    private JacksonTester<List<Product>> jsonRequestProductList;
    private JacksonTester<Product> jsonRequestProduct;

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
        product.setIngredients(new HashMap<>());

        response = new RecipeDTO();
        response.setItemName(productName);
        response.setIngredients(null);
        response.setTotalPrice(BigDecimal.valueOf(price));

        duration = Duration.of(timeout, ChronoUnit.MILLIS);

        resources.register(testConfigurationIT.getChannel(), duration);

        blockingStub =  testConfigurationIT.getBlockingStub();
    }

    @Test
    public void getRecipe_Should_returnRecipeDto_When_thereIsResponse() throws IOException {
        //given
        productBase.getDatabase().put(productName,product);
        resources.register(testConfigurationIT.startInProcessServiceForItemOrderBookResponse(), duration);
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
        productBase.getDatabase().put(productName,product);
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
        List<Product>products = new ArrayList<>();
        products.add(product);
        resources.register(testConfigurationIT.startInProcessServiceForInterestResponse(), duration);
        doReturn(blockingStub).when(auroraClientSideService).getBlockingStub();

        //when & then
        given()
                .contentType(ContentType.JSON)
                .body(products)
                .post(productControllerCreateProductUrl)
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .body(is(jsonRequestProduct.write(product).getJson() ));
    }

    @AfterEach
    void cleanUp() {
        File data = new File(databaseBackupUrl);
        if (data.length() > 0) {
            data.delete();
        }
    }
}
