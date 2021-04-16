package com.market.banica.calculator.controllerTests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.market.banica.calculator.controller.CalculatorController;
import com.market.banica.calculator.dto.ProductDto;
import com.market.banica.calculator.service.contract.CalculatorService;
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
import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(SpringExtension.class)
@WebMvcTest(CalculatorController.class)
public class CalculatorControllerTest {
    @MockBean
    CalculatorService service;
    @Autowired
    private MockMvc mockMvc;
    private JacksonTester<List<ProductDto>> jacksonResponseProductDtoList;

    @BeforeEach
    private void setUp() {
        JacksonTester.initFields(this, new ObjectMapper());
    }

    @Test
    void getRecipe() throws Exception {
        String clientId = "dummyClient";
        String product = "baklava";
        int quantity = 100;

        List<ProductDto> productDtoList = new ArrayList<>();
        ProductDto dummyRecipe = new ProductDto();
        dummyRecipe.setIngredients(new HashMap<>());
        dummyRecipe.setItemName("baklava");
        dummyRecipe.setTotalPrice(BigDecimal.valueOf(10));
        productDtoList.add(dummyRecipe);


        given(service.getRecipe(clientId, product, 100)).willReturn(productDtoList);


        MockHttpServletResponse response = mockMvc.perform(MockMvcRequestBuilders.get
                ("/calculator/" + clientId + "/" + product + "/" + quantity)
                .accept(MediaType.APPLICATION_JSON))
                .andReturn().getResponse();


        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.getContentAsString()).isEqualTo(jacksonResponseProductDtoList.write(productDtoList).getJson());
    }
}
