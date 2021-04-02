package com.market.banica.calculator.controllerTests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.market.banica.calculator.controller.ProductController;
import com.market.banica.calculator.model.Product;
import com.market.banica.calculator.service.contract.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@ExtendWith(MockitoExtension.class)
public class ProductControllerTests {

    private MockMvc mockMvc;

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController productController;

    private JacksonTester<List<Product>> jsonResponseListProduct;
    private JacksonTester<Product> jsonResponseProduct;

    @BeforeEach
    private void setUp() {
        JacksonTester.initFields(this, new ObjectMapper());
        mockMvc = MockMvcBuilders.standaloneSetup(productController).build();
    }

    @Test
    public void createProduct_Should_returnProduct() throws Exception {
        //given
        Product product = new Product();
        List<Product> products = new ArrayList<>(Collections.singletonList(product));
        given(productService.createProduct(products)).willReturn(product);

        //when
        MockHttpServletResponse response = mockMvc.perform(
                post("/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonResponseListProduct.write(products).getJson())
                        .accept(MediaType.APPLICATION_JSON))
                .andReturn().getResponse();

        //then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.getContentAsString())
                .isEqualTo(jsonResponseProduct.write(product).getJson());
    }
}
