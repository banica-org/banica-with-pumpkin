package com.market.banica.calculator.controller;

import com.market.banica.calculator.dto.RecipeDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.HashSet;

/**
 * Date: 3/10/2021 Time: 9:10 AM
 * <p>
 *
 * Tests for CalculatorController
 *
 * @author Vladislav_Zlatanov
 */

@ExtendWith(SpringExtension.class)
@WebMvcTest(CalculatorController.class)
public class CalculatorControllerTest {
    @Autowired
    private MockMvc mockMvc;


    //TODO:FINISH TEST AFTER SERVICE LAYER IS CREATED
    @Test
    void getRecipe() throws Exception {
        String clientId = "dummyClient";
        String product =  "baklava";
        int quantity = 100;


        RecipeDTO dummyRecipe = new RecipeDTO();
        dummyRecipe.setIngredients(new HashSet<>());
        dummyRecipe.setItemName("baklava");
        dummyRecipe.setTotalPrice(BigDecimal.valueOf(10));

        //mocked service should return dummyRecipe
        //when...


        MockHttpServletResponse response = mockMvc.perform(MockMvcRequestBuilders.get
                ("/calculator/" + clientId + "/" + product + "/" + quantity)
                .accept(MediaType.APPLICATION_JSON))
                .andReturn().getResponse();


        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
    }
}
