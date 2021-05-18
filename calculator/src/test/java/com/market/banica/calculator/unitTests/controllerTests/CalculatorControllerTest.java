package com.market.banica.calculator.unitTests.controllerTests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.market.banica.calculator.controller.CalculatorController;
import com.market.banica.calculator.dto.ItemDto;
import com.market.banica.calculator.dto.ProductDto;
import com.market.banica.calculator.model.Product;
import com.market.banica.calculator.service.contract.CalculatorService;
import com.market.banica.calculator.service.contract.TransactionService;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@ExtendWith(SpringExtension.class)
@WebMvcTest(CalculatorController.class)
public class CalculatorControllerTest {

    @MockBean
    CalculatorService calculatorService;

    @MockBean
    TransactionService transactionService;

    @Autowired
    private MockMvc mockMvc;

    private String clientId;
    private String product;
    private String location;
    private long quantity;
    private int totalPrice;
    private int productPrice;

    private static final String SELL_PRODUCT_MESSAGE = "Item with name %s was successfully sold to market.";

    private JacksonTester<List<ProductDto>> jacksonResponseProductDtoList;
    private JacksonTester<List<ItemDto>> jsonResponseListProduct;


    @BeforeEach
    private void setUp() {
        JacksonTester.initFields(this, new ObjectMapper());
        clientId = "dummyClient";
        product = "baklava";
        location = "Europe";
        quantity = 100;
        totalPrice = 10;
        productPrice = 3;
    }

    @Test
    void getRecipeShouldReturnTheRecipeWithBestPrice() throws Exception {
        List<ProductDto> productDtoList = new ArrayList<>();
        ProductDto dummyRecipe = getProductDto();
        productDtoList.add(dummyRecipe);

        given(calculatorService.getProduct(clientId, product, quantity)).willReturn(productDtoList);

        MockHttpServletResponse response = mockMvc
                .perform(MockMvcRequestBuilders
                        .get("/calculator/" + clientId + "/" + product + "/" + quantity)
                        .accept(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.getContentAsString())
                .isEqualTo(jacksonResponseProductDtoList
                        .write(productDtoList)
                        .getJson());
    }

    @Test
    void buyProductShouldReturnThePurchasedProductRecipe() throws Exception {
        List<ProductDto> productDtoList = new ArrayList<>();
        ProductDto dummyRecipe = getProductDto();
        productDtoList.add(dummyRecipe);

        given(transactionService.buyProduct(clientId, product, quantity)).willReturn(productDtoList);

        MockHttpServletResponse response = mockMvc
                .perform(MockMvcRequestBuilders
                        .get("/calculator/buy/" + clientId + "/" + product + "/" + quantity)
                        .accept(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.getContentAsString()).isEqualTo(jacksonResponseProductDtoList.write(productDtoList).getJson());
    }

    @Test
    void sellProductShouldSellTheSpecifiedProduct() throws Exception {
        List<ItemDto> itemsToSell = new ArrayList<>();
        itemsToSell.add(new ItemDto(product, BigDecimal.valueOf(productPrice), location, quantity));

        String sellProductBaklavaMessage = String.format("Item with name %s was successfully sold to market.", product);

        given(transactionService.sellProduct(itemsToSell)).willReturn(sellProductBaklavaMessage);

        MockHttpServletResponse response = mockMvc
                .perform(post("/calculator/sell/" + clientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonResponseListProduct.write(itemsToSell).getJson())
                        .accept(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.getContentAsString()).isEqualTo(sellProductBaklavaMessage);
    }

    @NotNull
    private ProductDto getProductDto() {
        ProductDto dummyRecipe = new ProductDto();
        dummyRecipe.setIngredients(new HashMap<>());
        dummyRecipe.setItemName(product);
        dummyRecipe.setTotalPrice(BigDecimal.valueOf(totalPrice));
        return dummyRecipe;
    }
}
