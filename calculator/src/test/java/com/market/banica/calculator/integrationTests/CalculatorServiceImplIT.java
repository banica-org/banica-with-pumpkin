package com.market.banica.calculator.integrationTests;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.market.Origin;
import com.market.banica.calculator.data.contract.ProductBase;
import com.market.banica.calculator.dto.ProductDto;
import com.market.banica.calculator.exception.exceptions.ProductNotAvailableException;
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
    private long tomatoesKetchupQuantity;
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

        waterQuantity = 1565000;
        onlyWaterQuantity = 5;
        waterKetchupQuantity = 200;
        tomatoesQuantity = 975000;
        tomatoesKetchupQuantity = 130;
        ketchupQuantity = 15000;
        onlyKetchupQuantity = 2;
        sugarQuantity = 15000;
        sauceQuantity = 300;
        eggsQuantity = 4800;
        crustsQuantity = 400;
        milkQuantity = 400;
        pumpkinQuantity = 600;
        banicaQuantity = 2;
    }

    @Test
    public void getProductShouldReturnSingletonListProductDtoWhenProductHasNoIngredients() throws ProductNotAvailableException {
        //given
        String expectedResult = "[ {\n" +
                "  \"itemName\" : \"water\",\n" +
                "  \"totalPrice\" : 0.05,\n" +
                "  \"productSpecifications\" : [ {\n" +
                "    \"price\" : 0.01,\n" +
                "    \"location\" : \"AMERICA\",\n" +
                "    \"quantity\" : 5\n" +
                "  } ],\n" +
                "  \"ingredients\" : { }\n" +
                "} ]";
        doReturn(getTestData1().get(water)).when(auroraClientSideService)
                .getIngredient(water, clientId, onlyWaterQuantity);

        //when
        List<ProductDto> actualResult = calculatorService.getProduct(clientId, water, onlyWaterQuantity);

        //then
        assertEquals(convertStringToListOfProductDto(expectedResult).toString(), actualResult.toString());
    }

    @Test
    public void getProductShouldReturnSingletonListProductDtoWhenProductHasIngredientsButBetterSinglePrice() throws ProductNotAvailableException {
        //given
        String expectedResult = "[ {\n" +
                "  \"itemName\" : \"banica\",\n" +
                "  \"totalPrice\" : 2000000.0,\n" +
                "  \"productSpecifications\" : [ {\n" +
                "    \"price\" : 1000000.0,\n" +
                "    \"location\" : \"AMERICA\",\n" +
                "    \"quantity\" : 2\n" +
                "  } ],\n" +
                "  \"ingredients\" : { }\n" +
                "} ]\n";
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
        assertEquals(convertStringToListOfProductDto(expectedResult).toString(), actualResult.toString());
    }

    @Test
    public void getProductWithOneLevelInheritanceShouldReturnListOfProductDtoWhenProductHasIngredientsWithBetterPrice() throws ProductNotAvailableException, JsonProcessingException {
        //given
        String expectedResult = "[ {\n" +
                "  \"itemName\" : \"ketchup\",\n" +
                "  \"totalPrice\" : 197.00,\n" +
                "  \"productSpecifications\" : [ ],\n" +
                "  \"ingredients\" : {\n" +
                "    \"water\" : 100,\n" +
                "    \"tomatoes\" : 65\n" +
                "  }\n" +
                "}, {\n" +
                "  \"itemName\" : \"tomatoes\",\n" +
                "  \"totalPrice\" : 97.5,\n" +
                "  \"productSpecifications\" : [ {\n" +
                "    \"price\" : 1.5,\n" +
                "    \"location\" : \"AMERICA\",\n" +
                "    \"quantity\" : 130\n" +
                "  } ],\n" +
                "  \"ingredients\" : { }\n" +
                "}, {\n" +
                "  \"itemName\" : \"water\",\n" +
                "  \"totalPrice\" : 1.00,\n" +
                "  \"productSpecifications\" : [ {\n" +
                "    \"price\" : 0.01,\n" +
                "    \"location\" : \"AMERICA\",\n" +
                "    \"quantity\" : 200\n" +
                "  } ],\n" +
                "  \"ingredients\" : { }\n" +
                "} ]\n";
        doReturn(getTestData1().get(ketchup)).when(auroraClientSideService)
                .getIngredient(ketchup, clientId, onlyKetchupQuantity);
        doReturn(getTestData1().get(water)).when(auroraClientSideService)
                .getIngredient(water, clientId, waterKetchupQuantity);
        doReturn(getTestData1().get(tomatoes)).when(auroraClientSideService)
                .getIngredient(tomatoes, clientId, tomatoesKetchupQuantity);

        //when
        List<ProductDto> actualResult = calculatorService.getProduct(clientId, ketchup,
                onlyKetchupQuantity);
        ObjectWriter objectWriter = new ObjectMapper().writerWithDefaultPrettyPrinter();
        System.out.println(objectWriter.writeValueAsString(actualResult));
        //then
        assertEquals(convertStringToListOfProductDto(expectedResult).toString(), actualResult.toString());
    }

    @Test
    public void getProductWithThreeLevelsInheritanceShouldReturnListOfProductDtoWhenProductHasIngredientsWithBetterPrice() throws JsonProcessingException, ProductNotAvailableException {
        //given
        String expectedResult = "[ {\n" +
                "  \"itemName\" : \"banica\",\n" +
                "  \"totalPrice\" : 2007030.00,\n" +
                "  \"productSpecifications\" : [ ],\n" +
                "  \"ingredients\" : {\n" +
                "    \"pumpkin\" : 300,\n" +
                "    \"milk\" : 200,\n" +
                "    \"crusts\" : 200,\n" +
                "    \"sauce\" : 150\n" +
                "  }\n" +
                "}, {\n" +
                "  \"itemName\" : \"tomatoes\",\n" +
                "  \"totalPrice\" : 129.5,\n" +
                "  \"productSpecifications\" : [ {\n" +
                "    \"price\" : 1.5,\n" +
                "    \"location\" : \"AMERICA\",\n" +
                "    \"quantity\" : 15000\n" +
                "  }, {\n" +
                "    \"price\" : 2.0,\n" +
                "    \"location\" : \"AMERICA\",\n" +
                "    \"quantity\" : 960000\n" +
                "  } ],\n" +
                "  \"ingredients\" : { }\n" +
                "}, {\n" +
                "  \"itemName\" : \"crusts\",\n" +
                "  \"totalPrice\" : 640.0,\n" +
                "  \"productSpecifications\" : [ {\n" +
                "    \"price\" : 3.2,\n" +
                "    \"location\" : \"AMERICA\",\n" +
                "    \"quantity\" : 400\n" +
                "  } ],\n" +
                "  \"ingredients\" : { }\n" +
                "}, {\n" +
                "  \"itemName\" : \"milk\",\n" +
                "  \"totalPrice\" : 350.0,\n" +
                "  \"productSpecifications\" : [ {\n" +
                "    \"price\" : 1.5,\n" +
                "    \"location\" : \"AMERICA\",\n" +
                "    \"quantity\" : 200\n" +
                "  }, {\n" +
                "    \"price\" : 2.0,\n" +
                "    \"location\" : \"AMERICA\",\n" +
                "    \"quantity\" : 200\n" +
                "  } ],\n" +
                "  \"ingredients\" : { }\n" +
                "}, {\n" +
                "  \"itemName\" : \"sauce\",\n" +
                "  \"totalPrice\" : 1002075.00,\n" +
                "  \"productSpecifications\" : [ ],\n" +
                "  \"ingredients\" : {\n" +
                "    \"water\" : 150,\n" +
                "    \"sugar\" : 50,\n" +
                "    \"ketchup\" : 50\n" +
                "  }\n" +
                "}, {\n" +
                "  \"itemName\" : \"sugar\",\n" +
                "  \"totalPrice\" : 100.0,\n" +
                "  \"productSpecifications\" : [ {\n" +
                "    \"price\" : 2.0,\n" +
                "    \"location\" : \"AMERICA\",\n" +
                "    \"quantity\" : 15000\n" +
                "  } ],\n" +
                "  \"ingredients\" : { }\n" +
                "}, {\n" +
                "  \"itemName\" : \"water\",\n" +
                "  \"totalPrice\" : 1.50,\n" +
                "  \"productSpecifications\" : [ {\n" +
                "    \"price\" : 0.01,\n" +
                "    \"location\" : \"AMERICA\",\n" +
                "    \"quantity\" : 45000\n" +
                "  } ],\n" +
                "  \"ingredients\" : { }\n" +
                "}, {\n" +
                "  \"itemName\" : \"water\",\n" +
                "  \"totalPrice\" : 2.08,\n" +
                "  \"productSpecifications\" : [ {\n" +
                "    \"price\" : 0.01,\n" +
                "    \"location\" : \"AMERICA\",\n" +
                "    \"quantity\" : 346250\n" +
                "  }, {\n" +
                "    \"price\" : 0.02,\n" +
                "    \"location\" : \"AMERICA\",\n" +
                "    \"quantity\" : 652083\n" +
                "  }, {\n" +
                "    \"price\" : 0.03,\n" +
                "    \"location\" : \"AMERICA\",\n" +
                "    \"quantity\" : 501667\n" +
                "  } ],\n" +
                "  \"ingredients\" : { }\n" +
                "}, {\n" +
                "  \"itemName\" : \"pumpkin\",\n" +
                "  \"totalPrice\" : 450.0,\n" +
                "  \"productSpecifications\" : [ {\n" +
                "    \"price\" : 1.5,\n" +
                "    \"location\" : \"AMERICA\",\n" +
                "    \"quantity\" : 600\n" +
                "  } ],\n" +
                "  \"ingredients\" : { }\n" +
                "}, {\n" +
                "  \"itemName\" : \"ketchup\",\n" +
                "  \"totalPrice\" : 6579.00,\n" +
                "  \"productSpecifications\" : [ ],\n" +
                "  \"ingredients\" : {\n" +
                "    \"water\" : 100,\n" +
                "    \"tomatoes\" : 65\n" +
                "  }\n" +
                "} ]";
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
        ObjectWriter objectWriter = new ObjectMapper().writerWithDefaultPrettyPrinter();
        System.out.println(objectWriter.writeValueAsString(actualResult));

        //then
        assertEquals(convertStringToListOfProductDto(expectedResult).toString(), actualResult.toString());
    }

    @Test
    public void getProductWithThreeLevelsInheritanceShouldReturnBetterPriceWhenIngredientInCorrectPosition() throws ProductNotAvailableException, JsonProcessingException {
        //given
        String expectedResult = "[ {\n" +
                "  \"itemName\" : \"banica\",\n" +
                "  \"totalPrice\" : 1983539.00,\n" +
                "  \"productSpecifications\" : [ ],\n" +
                "  \"ingredients\" : {\n" +
                "    \"pumpkin\" : 300,\n" +
                "    \"milk\" : 200,\n" +
                "    \"crusts\" : 200,\n" +
                "    \"sauce\" : 150\n" +
                "  }\n" +
                "}, {\n" +
                "  \"itemName\" : \"eggs\",\n" +
                "  \"totalPrice\" : 2.60,\n" +
                "  \"productSpecifications\" : [ {\n" +
                "    \"price\" : 0.15,\n" +
                "    \"location\" : \"AMERICA\",\n" +
                "    \"quantity\" : 1600\n" +
                "  }, {\n" +
                "    \"price\" : 0.25,\n" +
                "    \"location\" : \"AMERICA\",\n" +
                "    \"quantity\" : 3200\n" +
                "  } ],\n" +
                "  \"ingredients\" : { }\n" +
                "}, {\n" +
                "  \"itemName\" : \"crusts\",\n" +
                "  \"totalPrice\" : 620.00,\n" +
                "  \"productSpecifications\" : [ ],\n" +
                "  \"ingredients\" : {\n" +
                "    \"water\" : 50,\n" +
                //best price of water should be here due to same difference of total best price and
                // second total best price of water(20000*0.01)-(20000*0.02) = 200 when compared with sauce,
                // but before sauce when compared by name
                // quantity 20000 is calculated as 2(banica quantity)*200(crusts quantity)*50(water quantity) and
                // represents units of water contained in ordered product - banica
                "    \"eggs\" : 12\n" +
                "  }\n" +
                "}, {\n" +
                "  \"itemName\" : \"milk\",\n" +
                "  \"totalPrice\" : 350.0,\n" +
                "  \"productSpecifications\" : [ {\n" +
                "    \"price\" : 1.5,\n" +
                "    \"location\" : \"AMERICA\",\n" +
                "    \"quantity\" : 200\n" +
                "  }, {\n" +
                "    \"price\" : 2.0,\n" +
                "    \"location\" : \"AMERICA\",\n" +
                "    \"quantity\" : 200\n" +
                "  } ],\n" +
                "  \"ingredients\" : { }\n" +
                "}, {\n" +
                "  \"itemName\" : \"sauce\",\n" +
                "  \"totalPrice\" : 990349.50,\n" +
                "  \"productSpecifications\" : [ ],\n" +
                "  \"ingredients\" : {\n" +
                "    \"water\" : 150,\n" +
                "    \"sugar\" : 50,\n" +
                "    \"ketchup\" : 50\n" +
                "  }\n" +
                "}, {\n" +
                "  \"itemName\" : \"sugar\",\n" +
                "  \"totalPrice\" : 100.0,\n" +
                "  \"productSpecifications\" : [ {\n" +
                "    \"price\" : 2.0,\n" +
                "    \"location\" : \"AMERICA\",\n" +
                "    \"quantity\" : 15000\n" +
                "  } ],\n" +
                "  \"ingredients\" : { }\n" +
                "}, {\n" +
                "  \"itemName\" : \"water\",\n" +
                "  \"totalPrice\" : 0.50,\n" +
                "  \"productSpecifications\" : [ {\n" +
                "    \"price\" : 0.01,\n" +
                "    \"location\" : \"AMERICA\",\n" +
                "    \"quantity\" : 20000\n" +
                "  } ],\n" +
                "  \"ingredients\" : { }\n" +
                "}, {\n" +
                "  \"itemName\" : \"water\",\n" +
                "  \"totalPrice\" : 2.33,\n" +
                "  \"productSpecifications\" : [ {\n" +
                "    \"price\" : 0.01,\n" +
                "    \"location\" : \"AMERICA\",\n" +
                "    \"quantity\" : 20000\n" +
                "  }, {\n" +
                "    \"price\" : 0.02,\n" +
                "    \"location\" : \"AMERICA\",\n" +
                "    \"quantity\" : 25000\n" +
                "  } ],\n" +
                "  \"ingredients\" : { }\n" +
                "}, {\n" +
                "  \"itemName\" : \"pumpkin\",\n" +
                "  \"totalPrice\" : 450.0,\n" +
                "  \"productSpecifications\" : [ {\n" +
                "    \"price\" : 1.5,\n" +
                "    \"location\" : \"AMERICA\",\n" +
                "    \"quantity\" : 600\n" +
                "  } ],\n" +
                "  \"ingredients\" : { }\n" +
                "}, {\n" +
                "  \"itemName\" : \"ketchup\",\n" +
                "  \"totalPrice\" : 6500.0,\n" +
                // ketchup deliberately set as simple product so best price of
                // water is supposed to move to crusts as sauce difference in total price of water with best water price
                // and second best price is(40000*0.01 + 5000*0.02)-(20000*0.01 + 25000*0.02) = 200, while crusts has same total price
                // difference(20000*0.01)-(20000*0.02) = 200, but is before sauce in alphabetical order
                // quantity 45000 is calculated as 2(banica quantity)*150(sauce quantity)*150(water quantity) and
                // represents units of water contained in ordered product - banica
                "  \"productSpecifications\" : [ {\n" +
                "    \"price\" : 130.0,\n" +
                "    \"location\" : \"AMERICA\",\n" +
                "    \"quantity\" : 15000\n" +
                "  } ],\n" +
                "  \"ingredients\" : { }\n" +
                "} ]\n";

        doReturn(getTestData3().get(ketchup)).when(auroraClientSideService)
                .getIngredient(ketchup, clientId, ketchupQuantity);
        doReturn(getTestData3().get(water)).when(auroraClientSideService)
                .getIngredient(water, clientId, waterQuantity);
        doReturn(getTestData3().get(tomatoes)).when(auroraClientSideService)
                .getIngredient(tomatoes, clientId, tomatoesQuantity);
        doReturn(getTestData3().get(crusts)).when(auroraClientSideService)
                .getIngredient(crusts, clientId, crustsQuantity);
        doReturn(getTestData3().get(eggs)).when(auroraClientSideService)
                .getIngredient(eggs, clientId, eggsQuantity);
        doReturn(getTestData3().get(milk)).when(auroraClientSideService)
                .getIngredient(milk, clientId, milkQuantity);
        doReturn(getTestData3().get(sauce)).when(auroraClientSideService)
                .getIngredient(sauce, clientId, sauceQuantity);
        doReturn(getTestData3().get(banica)).when(auroraClientSideService)
                .getIngredient(banica, clientId, banicaQuantity);
        doReturn(getTestData3().get(pumpkin)).when(auroraClientSideService)
                .getIngredient(pumpkin, clientId, pumpkinQuantity);
        doReturn(getTestData3().get(sugar)).when(auroraClientSideService)
                .getIngredient(sugar, clientId, sugarQuantity);

        //when
        List<ProductDto> actualResult = calculatorService.getProduct(clientId, banica, banicaQuantity);
        ObjectWriter objectWriter = new ObjectMapper().writerWithDefaultPrettyPrinter();
        System.out.println(objectWriter.writeValueAsString(actualResult));
        //then
        assertEquals(convertStringToListOfProductDto(expectedResult).toString(), actualResult.toString());
    }

    public Map<String, ItemOrderBookResponse> getTestData1() {
        Map<String, ItemOrderBookResponse> data = new LinkedHashMap<>();
        addProductToDatabase(data, "banica", 1000000, 2);
        addProductToDatabase(data, "pumpkin", 1.5, 600);
        addProductToDatabase(data, "milk", 1.5, 200);
        addProductToDatabase(data, "milk", 2, 200);
        addProductToDatabase(data, "crusts", 3.9, 400);
        addProductToDatabase(data, "water", 0.01, 391250);
        addProductToDatabase(data, "water", 0.02, 652083);
        addProductToDatabase(data, "water", 0.03, 521667);
        addProductToDatabase(data, "eggs", 0.15, 1600);
        addProductToDatabase(data, "eggs", 0.25, 3200);
        addProductToDatabase(data, "sauce", 6700, 300);
        addProductToDatabase(data, "sugar", 2, 15000);
        addProductToDatabase(data, "ketchup", 132, 15000);
        addProductToDatabase(data, "tomatoes", 1.5, 15000);
        addProductToDatabase(data, "tomatoes", 2, 960000);
        return data;
    }

    public Map<String, ItemOrderBookResponse> getTestData2() {
        Map<String, ItemOrderBookResponse> data = new LinkedHashMap<>();
        addProductToDatabase(data, "banica", 1005000, 2);
        addProductToDatabase(data, "pumpkin", 1.5, 600);
        addProductToDatabase(data, "milk", 1.5, 200);
        addProductToDatabase(data, "milk", 2, 200);
        addProductToDatabase(data, "crusts", 3.2, 400);
        addProductToDatabase(data, "water", 0.01, 391250);
        addProductToDatabase(data, "water", 0.02, 652083);
        addProductToDatabase(data, "water", 0.03, 521667);
        addProductToDatabase(data, "eggs", 0.15, 1600);
        addProductToDatabase(data, "eggs", 0.25, 3200);
        addProductToDatabase(data, "sauce", 6700, 300);
        addProductToDatabase(data, "sugar", 2, 15000);
        addProductToDatabase(data, "ketchup", 132, 15000);
        addProductToDatabase(data, "tomatoes", 1.5, 15000);
        addProductToDatabase(data, "tomatoes", 2, 960000);
        return data;
    }

    public Map<String, ItemOrderBookResponse> getTestData3() {
        Map<String, ItemOrderBookResponse> data = new LinkedHashMap<>();
        addProductToDatabase(data, "banica", 1000000, 2);
        //pumpkin:450,milk:350,crusts:620,sauce:990349.5=991769.5//better composite price//991832.5*2=1983539
        addProductToDatabase(data, "pumpkin", 1.5, 600);
        addProductToDatabase(data, "milk", 1.5, 200);
        addProductToDatabase(data, "milk", 2, 200);
        addProductToDatabase(data, "crusts", 3.6, 400);
        //water:(50*0.01)0.5,eggs:2.6=3.1//better composite price//3.1*200=620
        addProductToDatabase(data, "water", 0.01, 40000);
        addProductToDatabase(data, "water", 0.02, 1003333);
        addProductToDatabase(data, "water", 0.03, 521667);
        addProductToDatabase(data, "eggs", 0.15, 1600);
        addProductToDatabase(data, "eggs", 0.25, 3200);
        addProductToDatabase(data, "sauce", 6602.34, 300);
        //ketchup:6500,water:(66*0.01 + 84*0.02)2.33,sugar:100 =6602.75//better composite price//6602.33*150=990349.5
        addProductToDatabase(data, "sugar", 2, 15000);
        addProductToDatabase(data, "ketchup", 130, 15000);
        //tomatoes:129.5,water:(75*0.01 + 25*0.02)1.25 = 130.75//better simple product price//130*50=6500
        addProductToDatabase(data, "tomatoes", 1.5, 15000);
        addProductToDatabase(data, "tomatoes", 2, 960000);
        return data;
    }

    private void addProductToDatabase(Map<String, ItemOrderBookResponse> data, String productName, double price, int quantity) {

        ItemOrderBookResponse item = generateOrderBookResponse(productName, price, quantity);

        data.merge(productName, item, (a, b) -> {

            OrderBookLayer orderBookLayerB = b.getOrderbookLayersList().get(0);

            return ItemOrderBookResponse.newBuilder()
                    .setItemName(productName)
                    .addAllOrderbookLayers(a.getOrderbookLayersList())
                    .addOrderbookLayers(orderBookLayerB)
                    .build();
        });
    }

    private ItemOrderBookResponse generateOrderBookResponse(String productName, double price, int quantity) {

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
        Type productDtoListType = new TypeToken<ArrayList<ProductDto>>() {
        }.getType();

        return gson.fromJson(data,
                productDtoListType);
    }

    private ConcurrentHashMap<String, Product> getProductDataAsMap() {
        Gson gson = new Gson();
        String productNameProductMapAsJson = getProductDataAsString();
        Type productNameProductMapType = new TypeToken<ConcurrentHashMap<String, Product>>() {
        }.getType();
        ConcurrentHashMap<String, Product> data = gson.fromJson(productNameProductMapAsJson,
                productNameProductMapType);
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
