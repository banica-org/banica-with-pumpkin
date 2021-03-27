package com.market.banica.calculator.componentTests;

import com.asarkar.grpc.test.GrpcCleanupExtension;
import com.asarkar.grpc.test.Resources;
import com.aurora.Aurora;
import com.aurora.AuroraServiceGrpc;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.market.banica.calculator.CalculatorApplication;
import com.market.banica.calculator.componentTests.configuration.TestServiceIT;
import com.market.banica.calculator.controller.ProductController;
import com.market.banica.calculator.data.contract.ProductBase;
import com.market.banica.calculator.dto.RecipeDTO;
import com.market.banica.calculator.enums.UnitOfMeasure;
import com.market.banica.calculator.model.Product;
import com.market.banica.calculator.service.contract.JMXServiceMBean;
import com.market.banica.calculator.service.grpc.AuroraClientSideService;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessServerBuilder;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = CalculatorApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith({GrpcCleanupExtension.class})
@ActiveProfiles("testIT")
public class CalculatorComponentIT {

    @LocalServerPort
    private int port;

    @Autowired
    private ProductController productController;

    @Autowired
    private JMXServiceMBean jmxService;

    @Autowired
    private ProductBase productBase;

    @Autowired
    private TestServiceIT testServiceIT;

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

    @Value(value = "${order-book-topic-prefix}")
    private String orderBookTopicPrefix;

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
    public void SetUp() {
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

        resources.register(testServiceIT.getChannel(), duration);

        blockingStub = testServiceIT.getBlockingStub();
    }

    @Test
    public void getRecipe_Should_returnRecipeDto_When_thereIsResponse() throws IOException {
        //given
        productBase.getDatabase().put(productName, product);
        resources.register(testServiceIT.startInProcessServiceForItemOrderBookResponse(), duration);
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
        productBase.getDatabase().put(productName, product);
        resources.register(testServiceIT.startInProcessServiceWithEmptyService(), duration);
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
        List<Product> products = Collections.singletonList(product);
        resources.register(testServiceIT.startInProcessServiceForInterestResponse(), duration);
        doReturn(blockingStub).when(auroraClientSideService).getBlockingStub();

        //when & then
        given()
                .contentType(ContentType.JSON)
                .body(products)
                .post(productControllerCreateProductUrl)
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .body(is(jsonRequestProduct.write(product).getJson()));
    }

    @Test
    public void createProduct_Should_returnError_When_thereIsNoResponse() throws IOException {
        //given
        List<Product> products = Collections.singletonList(product);
        resources.register(testServiceIT.startInProcessServiceWithEmptyService(), duration);
        doReturn(blockingStub).when(auroraClientSideService).getBlockingStub();

        //when & then
        given()
                .contentType(ContentType.JSON)
                .body(products)
                .post(productControllerCreateProductUrl)
                .then()
                .assertThat()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    @Test
    public void deleteProduct_Should_sendRequestWithProductName_When_thereIsService() throws IOException {
        //given
        ArgumentCaptor<Aurora.AuroraRequest> requestCaptor = ArgumentCaptor.forClass(Aurora.AuroraRequest.class);

        AuroraServiceGrpc.AuroraServiceImplBase server = testServiceIT.getGrpcServiceForCancelSubscriptionResponse();
        resources.register(InProcessServerBuilder.forName(testServiceIT.getServerName()).directExecutor()
                .addService(server).build().start(), duration);

        productBase.getDatabase().put(productName, product);

        doReturn(blockingStub).when(auroraClientSideService).getBlockingStub();

        //when
        jmxService.deleteProductFromDatabase(productName);

        //then

        verify(server)
                .request(requestCaptor.capture(), ArgumentMatchers.any());
        assertEquals(orderBookTopicPrefix + productName, requestCaptor.getValue().getTopic());
    }

    @Test
    public void deleteProduct_Should_returnError_When_thereIsNoService() throws IOException {
        //given
        productBase.getDatabase().put(productName, product);
        resources.register(testServiceIT.startInProcessServiceWithEmptyService(), duration);
        doReturn(blockingStub).when(auroraClientSideService).getBlockingStub();

        //when & then
        assertThrows(StatusRuntimeException.class, () -> jmxService.deleteProductFromDatabase(productName));
    }

    @AfterEach
    void cleanUp() {
        productBase.getDatabase().clear();
        File data = new File(databaseBackupUrl);
        if (data.length() > 0) {
            data.delete();
        }
    }
}
