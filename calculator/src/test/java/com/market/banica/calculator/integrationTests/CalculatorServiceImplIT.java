package com.market.banica.calculator.integrationTests;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.market.Origin;
import com.market.banica.calculator.data.contract.ProductBase;
import com.market.banica.calculator.dto.ProductDto;
import com.market.banica.calculator.dto.ProductSpecification;
import com.market.banica.calculator.model.Product;
import com.market.banica.calculator.service.contract.BackUpService;
import com.market.banica.calculator.service.contract.CalculatorService;
import com.market.banica.calculator.service.grpc.AuroraClientSideService;
import com.orderbook.ItemOrderBookResponse;
import com.orderbook.OrderBookLayer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@SpringBootTest
@TestPropertySource(locations = "/application-testIT.properties")
public class CalculatorServiceImplIT {

    @Autowired
    private CalculatorService calculatorService;

    @Autowired
    private ProductBase productBase;

    @MockBean
    private AuroraClientSideService auroraClientSideService;

    @MockBean
    private BackUpService backUpService;


    private String clientId;
    private String productName;
    private long productQuantity;
    private double price;

    @BeforeEach
    public void setUp() {
        productBase.getDatabase().putAll(getProductDataAsMap());

        clientId = "1";
        productName = "water";
        productQuantity = 2;
        price = 2.5;
    }

    @Test
    public void getProductShouldReturnSingletonListProductDtoWhenProductHasNoIngredients() throws Exception {
        //given
        List<ProductDto> expectedResult = new ArrayList<>();
        when(auroraClientSideService.getIngredient(productName, clientId, productQuantity))
                .thenReturn(getTestData1().get(productName));

        //when
        List<ProductDto> actualResult = calculatorService.getProduct(clientId, productName, productQuantity);

        //then
        assertEquals(actualResult, expectedResult);
    }

    @NotNull
    private ProductDto createProductDTO() {
        ProductDto response = new ProductDto();
        ProductSpecification productSpecification = new ProductSpecification();

        productSpecification.setLocation("AMERICA");
        productSpecification.setPrice(BigDecimal.valueOf(price));
        productSpecification.setQuantity(productQuantity);

        BigDecimal totalPrice = BigDecimal.valueOf(price * productQuantity);

        response.setItemName(productName);
        response.setTotalPrice(totalPrice);
        response.setProductSpecifications(Collections.singletonList(productSpecification));

        return response;
    }

    public Map<String, ItemOrderBookResponse> getTestData1() {
        Map<String, ItemOrderBookResponse> data = new LinkedHashMap<>();
        addProductToDatabase(data, "banica", 100000, 2);//pumpkin:450,milk:350,crusts:620,sauce:1009162,5=1010582,5*2=2021165
        addProductToDatabase(data, "pumpkin", 1.5, 300);
        addProductToDatabase(data, "milk", 1.5, 100);
        addProductToDatabase(data, "milk", 2, 100);
        addProductToDatabase(data, "crusts", 3.9, 200);//water:0,5,eggs:2.6=3.1 * 200 = 620
        addProductToDatabase(data, "water", 0.01, 75);
        addProductToDatabase(data, "water", 0.02, 125);
        addProductToDatabase(data, "water", 0.03, 100);
        addProductToDatabase(data, "eggs", 0.15, 4);
        addProductToDatabase(data, "eggs", 0.25, 8);
        addProductToDatabase(data, "sauce", 7000, 150);//ketchup:6625,water:2.75,sugar:100 =6727.75 * 150=1009162,5
        addProductToDatabase(data, "sugar", 2, 50);
        addProductToDatabase(data, "ketchup", 132, 50);//tomatoes:129.5,water:3 = 132.5 * 50 = 6625
        addProductToDatabase(data, "tomatoes", 1.5, 1);
        addProductToDatabase(data, "tomatoes", 2, 64);
        return data;
    }

    private void addProductToDatabase(Map<String, ItemOrderBookResponse> data, String productName, double price, int quantity) {

        ItemOrderBookResponse item = generateItoOrderBookResponse(productName, price, quantity);

        data.merge(productName, item, (a, b) -> {

            OrderBookLayer orderBookLayerB = b.getOrderbookLayersList().get(0);

            return ItemOrderBookResponse.newBuilder()
                    .setItemName(productName)
                    .addAllOrderbookLayers(a.getOrderbookLayersList())
                    .addOrderbookLayers(orderBookLayerB)
                    .build();
        });

    }

    private ItemOrderBookResponse generateItoOrderBookResponse(String productName, double price, int quantity) {

        List<OrderBookLayer> list = new ArrayList<>();
        list.add(generateOrderBookLayer(price, quantity));

        return ItemOrderBookResponse
                .newBuilder()
                .setItemName(productName)
                .addAllOrderbookLayers(list)
                .build();

    }

    private OrderBookLayer generateOrderBookLayer(double price, long quantity) {

        return OrderBookLayer.newBuilder().setPrice(price).setQuantity(quantity).setOrigin(getRandomOrigin()).build();

    }

    private Origin getRandomOrigin() {
        return Origin.forNumber(new Random().nextInt(4));
    }

    private static ConcurrentHashMap<String, Product> getProductDataAsMap() {
        Gson gson = new Gson();
        String productSpecificationListAsJson = getProductDataAsString();
        Type productSpecificationListType = new TypeToken<ConcurrentHashMap<String, Product>>() {
        }.getType();
        ConcurrentHashMap<String, Product> data = gson.fromJson(productSpecificationListAsJson,
                productSpecificationListType);
        return data;
    }

    private static String getProductDataAsString() {
        return "{\n" +
                "  \"crusts\" : {\n" +
                "    \"productName\" : \"crusts\",\n" +
                "    \"unitOfMeasure\" : \"GRAM\",\n" +
                "    \"ingredients\" : {\n" +
                "      \"water\" : 50,\n" +
                "      \"eggs\" : 12\n" +
                "    }\n" +
                "  },\n" +
                "  \"eggs\" : {\n" +
                "    \"productName\" : \"eggs\",\n" +
                "    \"unitOfMeasure\" : \"PIECE\",\n" +
                "    \"ingredients\" : { }\n" +
                "  },\n" +
                "  \"tomatoes\" : {\n" +
                "    \"productName\" : \"tomatoes\",\n" +
                "    \"unitOfMeasure\" : \"GRAM\",\n" +
                "    \"ingredients\" : { }\n" +
                "  },\n" +
                "  \"milk\" : {\n" +
                "    \"productName\" : \"milk\",\n" +
                "    \"unitOfMeasure\" : \"MILLILITER\",\n" +
                "    \"ingredients\" : { }\n" +
                "  },\n" +
                "  \"sauce\" : {\n" +
                "    \"productName\" : \"sauce\",\n" +
                "    \"unitOfMeasure\" : \"GRAM\",\n" +
                "    \"ingredients\" : {\n" +
                "      \"water\" : 150,\n" +
                "      \"sugar\" : 50,\n" +
                "      \"ketchup\" : 50\n" +
                "    }\n" +
                "  },\n" +
                "  \"banica\" : {\n" +
                "    \"productName\" : \"banica\",\n" +
                "    \"unitOfMeasure\" : \"GRAM\",\n" +
                "    \"ingredients\" : {\n" +
                "      \"pumpkin\" : 300,\n" +
                "      \"milk\" : 200,\n" +
                "      \"crusts\" : 200,\n" +
                "      \"sauce\" : 150\n" +
                "    }\n" +
                "  },\n" +
                "  \"pumpkin\" : {\n" +
                "    \"productName\" : \"pumpkin\",\n" +
                "    \"unitOfMeasure\" : \"GRAM\",\n" +
                "    \"ingredients\" : { }\n" +
                "  },\n" +
                "  \"water\" : {\n" +
                "    \"productName\" : \"water\",\n" +
                "    \"unitOfMeasure\" : \"MILLILITER\",\n" +
                "    \"ingredients\" : { }\n" +
                "  },\n" +
                "  \"sugar\" : {\n" +
                "    \"productName\" : \"sugar\",\n" +
                "    \"unitOfMeasure\" : \"GRAM\",\n" +
                "    \"ingredients\" : { }\n" +
                "  },\n" +
                "  \"ketchup\" : {\n" +
                "    \"productName\" : \"ketchup\",\n" +
                "    \"unitOfMeasure\" : \"MILLILITER\",\n" +
                "    \"ingredients\" : {\n" +
                "      \"water\" : 100,\n" +
                "      \"tomatoes\" : 65\n" +
                "    }\n" +
                "  }\n" +
                "}";
    }

}
