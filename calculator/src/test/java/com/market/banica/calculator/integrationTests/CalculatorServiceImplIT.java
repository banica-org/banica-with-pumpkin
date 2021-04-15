package com.market.banica.calculator.integrationTests;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.market.Origin;
import com.market.banica.calculator.data.contract.ProductBase;
import com.market.banica.calculator.dto.ProductDto;
import com.market.banica.calculator.model.Product;
import com.market.banica.calculator.service.CalculatorServiceImpl;
import com.market.banica.calculator.service.contract.BackUpService;
import com.market.banica.calculator.service.grpc.AuroraClientSideService;
import com.orderbook.ItemOrderBookResponse;
import com.orderbook.OrderBookLayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.TestPropertySource;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;

@SpringBootTest
@TestPropertySource(locations = "/application-testIT.properties")
public class CalculatorServiceImplIT {

    @Autowired
    @InjectMocks
    private CalculatorServiceImpl calculatorService;

    @Autowired
    private ProductBase productBase;

    @Autowired
    @SpyBean
    private AuroraClientSideService auroraClientSideService;

    @MockBean
    private BackUpService backUpService;


    private String clientId;
    private String location;

    private String water;
    private String tomatoes;
    private String ketchup;
    private String sugar;
    private String sauce;
    private String eggs;
    private String crusts;
    private String milk;
    private String pumpkin;
    private String banica;

    private long waterQuantity;
    private long onlyWaterQuantity;
    private long waterKetchupQuantity;
    private long tomatoesQuantity;
    private long ketchupQuantity;
    private long onlyKetchupQuantity;
    private long sugarQuantity;
    private long sauceQuantity;
    private long eggsQuantity;
    private long crustsQuantity;
    private long milkQuantity;
    private long pumpkinQuantity;
    private long banicaQuantity;

    @BeforeEach
    public void setUp() {
        productBase.getDatabase().putAll(getProductDataAsMap());

        clientId = "1";
        location = "AMERICA";

        water = "water";
        tomatoes = "tomatoes";
        ketchup = "ketchup";
        sugar = "sugar";
        eggs = "eggs";
        crusts = "crusts";
        milk = "milk";
        pumpkin = "pumpkin";
        banica = "banica";
        sauce = "sauce";

        waterQuantity = 300;
        onlyWaterQuantity = 5;
        waterKetchupQuantity = 100;
        tomatoesQuantity = 65;
        ketchupQuantity = 50;
        onlyKetchupQuantity = 2;
        sugarQuantity = 50;
        sauceQuantity = 150;
        eggsQuantity = 12;
        crustsQuantity = 200;
        milkQuantity = 200;
        pumpkinQuantity = 300;
        banicaQuantity = 2;
    }

    @Test
    public void getProductShouldReturnSingletonListProductDtoWhenProductHasNoIngredients() {
        //given
        String expectedResult = "[{" +
                "\"itemName\":\"water\"," +
                "\"totalPrice\":0.05," +
                "\"productSpecifications\":[{" +
                "      \"price\":0.01," +
                "      \"location\":\"AMERICA\"," +
                "      \"quantity\":5}]," +
                "\"ingredients\":{}" +
                "}]";
        doReturn(getTestData1().get(water)).when(auroraClientSideService)
                .getIngredient(water, clientId, onlyWaterQuantity);

        //when
        List<ProductDto> actualResult = calculatorService.getProduct(clientId, water, onlyWaterQuantity);

        //then
        assertEquals(convertStringToListOfProductDto(expectedResult), actualResult);
    }

    @Test
    public void getProductShouldReturnSingletonListProductDtoWhenProductHasIngredientsButBetterSinglePrice() {
        //given
        String expectedResult = "[{" +
                "\"itemName\":\"banica\"," +
                "\"totalPrice\":200000.0," +
                "\"productSpecifications\":[{" +
                "      \"price\":100000.0," +
                "      \"location\":\"AMERICA\"," +
                "      \"quantity\":2}]," +
                "\"ingredients\":{}" +
                "}]";
        doReturn(getTestData1().get(ketchup)).when(auroraClientSideService)
                .getIngredient(ketchup, clientId, ketchupQuantity);
        doReturn(getTestData1().get(water)).when(auroraClientSideService)
                .getIngredient(water, clientId, waterQuantity);
        doReturn(getTestData1().get(tomatoes)).when(auroraClientSideService)
                .getIngredient(tomatoes, clientId, tomatoesQuantity);
        doReturn(getTestData1().get(crusts)).when(auroraClientSideService)
                .getIngredient(crusts, clientId, crustsQuantity);
        doReturn(getTestData1().get(eggs)).when(auroraClientSideService)
                .getIngredient(eggs, clientId, eggsQuantity);
        doReturn(getTestData1().get(milk)).when(auroraClientSideService)
                .getIngredient(milk, clientId, milkQuantity);
        doReturn(getTestData1().get(sauce)).when(auroraClientSideService)
                .getIngredient(sauce, clientId, sauceQuantity);
        doReturn(getTestData1().get(banica)).when(auroraClientSideService)
                .getIngredient(banica, clientId, banicaQuantity);
        doReturn(getTestData1().get(pumpkin)).when(auroraClientSideService)
                .getIngredient(pumpkin, clientId, pumpkinQuantity);
        doReturn(getTestData1().get(sugar)).when(auroraClientSideService)
                .getIngredient(sugar, clientId, sugarQuantity);

        //when
        List<ProductDto> actualResult = calculatorService.getProduct(clientId, banica, banicaQuantity);

        //then
        assertEquals(convertStringToListOfProductDto(expectedResult), actualResult);
    }

    @Test
    public void getProductWithOneLevelInheritanceShouldReturnListOfProductDtoWhenProductHasIngredientsWithBetterPrice() {
        //given
        String expectedResult = "[ {\n" +
                "  \"itemName\" : \"ketchup\",\n" +
                "  \"totalPrice\" : 261.50,\n" +
                "  \"productSpecifications\" : [ ],\n" +
                "  \"ingredients\" : {\n" +
                "    \"water\" : 100,\n" +
                "    \"tomatoes\" : 65\n" +
                "  }\n" +
                "}, {\n" +
                "  \"itemName\" : \"tomatoes\",\n" +
                "  \"totalPrice\" : 129.5,\n" +
                "  \"productSpecifications\" : [ {\n" +
                "    \"price\" : 1.5,\n" +
                "    \"location\" : \"AMERICA\",\n" +
                "    \"quantity\" : 1\n" +
                "  }, {\n" +
                "    \"price\" : 2.0,\n" +
                "    \"location\" : \"AMERICA\",\n" +
                "    \"quantity\" : 64\n" +
                "  } ],\n" +
                "  \"ingredients\" : { }\n" +
                "}, {\n" +
                "  \"itemName\" : \"water\",\n" +
                "  \"totalPrice\" : 1.25,\n" +
                "  \"productSpecifications\" : [ {\n" +
                "    \"price\" : 0.01,\n" +
                "    \"location\" : \"AMERICA\",\n" +
                "    \"quantity\" : 75\n" +
                "  }, {\n" +
                "    \"price\" : 0.02,\n" +
                "    \"location\" : \"AMERICA\",\n" +
                "    \"quantity\" : 25\n" +
                "  } ],\n" +
                "  \"ingredients\" : { }\n" +
                "} ]";
        doReturn(getTestData1().get(ketchup)).when(auroraClientSideService)
                .getIngredient(ketchup, clientId, onlyKetchupQuantity);
        doReturn(getTestData1().get(water)).when(auroraClientSideService)
                .getIngredient(water, clientId, waterKetchupQuantity);
        doReturn(getTestData1().get(tomatoes)).when(auroraClientSideService)
                .getIngredient(tomatoes, clientId, tomatoesQuantity);

        //when
        List<ProductDto> actualResult = calculatorService.getProduct(clientId, ketchup,
                onlyKetchupQuantity);

        //then
        assertEquals(convertStringToListOfProductDto(expectedResult), actualResult);
    }

    @Test
    public void getProductWithThreeLevelsInheritanceShouldReturnListOfProductDtoWhenProductHasIngredientsWithBetterPrice() {
        //given
        String expectedResult = "[\n" +
                "    {\n" +
                "        \"itemName\": \"banica\",\n" +
                "        \"totalPrice\": 1995460.00,\n" +
                "        \"productSpecifications\": [],\n" +
                "        \"ingredients\": {\n" +
                "            \"pumpkin\": 300,\n" +
                "            \"milk\": 200,\n" +
                "            \"crusts\": 200,\n" +
                "            \"sauce\": 150\n" +
                "        }\n" +
                "    },\n" +
                "    {\n" +
                "        \"itemName\": \"tomatoes\",\n" +
                "        \"totalPrice\": 129.5,\n" +
                "        \"productSpecifications\": [\n" +
                "            {\n" +
                "                \"price\": 1.5,\n" +
                "                \"location\": \"EUROPE\",\n" +
                "                \"quantity\": 1\n" +
                "            },\n" +
                "            {\n" +
                "                \"price\": 2.0,\n" +
                "                \"location\": \"EUROPE\",\n" +
                "                \"quantity\": 64\n" +
                "            }\n" +
                "        ],\n" +
                "        \"ingredients\": {}\n" +
                "    },\n" +
                "    {\n" +
                "        \"itemName\": \"crusts\",\n" +
                "        \"totalPrice\": 780.0,\n" +
                "        \"productSpecifications\": [\n" +
                "            {\n" +
                "                \"price\": 3.9,\n" +
                "                \"location\": \"EUROPE\",\n" +
                "                \"quantity\": 200\n" +
                "            }\n" +
                "        ],\n" +
                "        \"ingredients\": {}\n" +
                "    },\n" +
                "    {\n" +
                "        \"itemName\": \"milk\",\n" +
                "        \"totalPrice\": 350.0,\n" +
                "        \"productSpecifications\": [\n" +
                "            {\n" +
                "                \"price\": 1.5,\n" +
                "                \"location\": \"ASIA\",\n" +
                "                \"quantity\": 100\n" +
                "            },\n" +
                "            {\n" +
                "                \"price\": 2.0,\n" +
                "                \"location\": \"AMERICA\",\n" +
                "                \"quantity\": 100\n" +
                "            }\n" +
                "        ],\n" +
                "        \"ingredients\": {}\n" +
                "    },\n" +
                "    {\n" +
                "        \"itemName\": \"sauce\",\n" +
                "        \"totalPrice\": 996150.00,\n" +
                "        \"productSpecifications\": [],\n" +
                "        \"ingredients\": {\n" +
                "            \"water\": 150,\n" +
                "            \"sugar\": 50,\n" +
                "            \"ketchup\": 50\n" +
                "        }\n" +
                "    },\n" +
                "    {\n" +
                "        \"itemName\": \"sugar\",\n" +
                "        \"totalPrice\": 100.0,\n" +
                "        \"productSpecifications\": [\n" +
                "            {\n" +
                "                \"price\": 2.0,\n" +
                "                \"location\": \"ASIA\",\n" +
                "                \"quantity\": 50\n" +
                "            }\n" +
                "        ],\n" +
                "        \"ingredients\": {}\n" +
                "    },\n" +
                "    {\n" +
                "        \"itemName\": \"water\",\n" +
                "        \"totalPrice\": 3.50,\n" +
                "        \"productSpecifications\": [\n" +
                "            {\n" +
                "                \"price\": 0.02,\n" +
                "                \"location\": \"EUROPE\",\n" +
                "                \"quantity\": 100\n" +
                "            },\n" +
                "            {\n" +
                "                \"price\": 0.03,\n" +
                "                \"location\": \"UNSPECIFIED\",\n" +
                "                \"quantity\": 50\n" +
                "            }\n" +
                "        ],\n" +
                "        \"ingredients\": {}\n" +
                "    },\n" +
                "    {\n" +
                "        \"itemName\": \"water\",\n" +
                "        \"totalPrice\": 1.25,\n" +
                "        \"productSpecifications\": [\n" +
                "            {\n" +
                "                \"price\": 0.01,\n" +
                "                \"location\": \"EUROPE\",\n" +
                "                \"quantity\": 75\n" +
                "            },\n" +
                "            {\n" +
                "                \"price\": 0.02,\n" +
                "                \"location\": \"EUROPE\",\n" +
                "                \"quantity\": 25\n" +
                "            }\n" +
                "        ],\n" +
                "        \"ingredients\": {}\n" +
                "    },\n" +
                "    {\n" +
                "        \"itemName\": \"pumpkin\",\n" +
                "        \"totalPrice\": 450.0,\n" +
                "        \"productSpecifications\": [\n" +
                "            {\n" +
                "                \"price\": 1.5,\n" +
                "                \"location\": \"UNSPECIFIED\",\n" +
                "                \"quantity\": 300\n" +
                "            }\n" +
                "        ],\n" +
                "        \"ingredients\": {}\n" +
                "    },\n" +
                "    {\n" +
                "        \"itemName\": \"ketchup\",\n" +
                "        \"totalPrice\": 6537.50,\n" +
                "        \"productSpecifications\": [],\n" +
                "        \"ingredients\": {\n" +
                "            \"water\": 100,\n" +
                "            \"tomatoes\": 65\n" +
                "        }\n" +
                "    }\n" +
                "]\n";
        doReturn(getTestData2().get(ketchup)).when(auroraClientSideService)
                .getIngredient(ketchup, clientId, ketchupQuantity);
        doReturn(getTestData2().get(water)).when(auroraClientSideService)
                .getIngredient(water, clientId, waterQuantity);
        doReturn(getTestData2().get(tomatoes)).when(auroraClientSideService)
                .getIngredient(tomatoes, clientId, tomatoesQuantity);
        doReturn(getTestData2().get(crusts)).when(auroraClientSideService)
                .getIngredient(crusts, clientId, crustsQuantity);
        doReturn(getTestData2().get(eggs)).when(auroraClientSideService)
                .getIngredient(eggs, clientId, eggsQuantity);
        doReturn(getTestData2().get(milk)).when(auroraClientSideService)
                .getIngredient(milk, clientId, milkQuantity);
        doReturn(getTestData2().get(sauce)).when(auroraClientSideService)
                .getIngredient(sauce, clientId, sauceQuantity);
        doReturn(getTestData2().get(banica)).when(auroraClientSideService)
                .getIngredient(banica, clientId, banicaQuantity);
        doReturn(getTestData2().get(pumpkin)).when(auroraClientSideService)
                .getIngredient(pumpkin, clientId, pumpkinQuantity);
        doReturn(getTestData2().get(sugar)).when(auroraClientSideService)
                .getIngredient(sugar, clientId, sugarQuantity);

        //when
        List<ProductDto> actualResult = calculatorService.getProduct(clientId, banica, banicaQuantity);

        //then
        assertEquals(convertStringToListOfProductDto(expectedResult), actualResult);
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

    public Map<String, ItemOrderBookResponse> getTestData2() {
        Map<String, ItemOrderBookResponse> data = new LinkedHashMap<>();
        addProductToDatabase(data, "banica", 1000000, 2);//pumpkin:450,milk:350,crusts:620,sauce:1009162,5=1010582,5*2=2021165
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

        return OrderBookLayer.newBuilder().setPrice(price).setQuantity(quantity).setOrigin(Origin.AMERICA).build();
    }

    private List<ProductDto> convertStringToListOfProductDto(String data) {

        Gson gson = new Gson();
        Type productSpecificationListType = new TypeToken<ArrayList<ProductDto>>() {
        }.getType();

        return gson.fromJson(data,
                productSpecificationListType);
    }

    private ConcurrentHashMap<String, Product> getProductDataAsMap() {
        Gson gson = new Gson();
        String productSpecificationListAsJson = getProductDataAsString();
        Type productSpecificationListType = new TypeToken<ConcurrentHashMap<String, Product>>() {
        }.getType();
        ConcurrentHashMap<String, Product> data = gson.fromJson(productSpecificationListAsJson,
                productSpecificationListType);
        return data;
    }

    private String getProductDataAsString() {
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
