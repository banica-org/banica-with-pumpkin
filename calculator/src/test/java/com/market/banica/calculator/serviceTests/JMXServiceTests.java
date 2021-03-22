package com.market.banica.calculator.serviceTests;

import com.market.banica.calculator.data.contract.ProductBase;
import com.market.banica.calculator.enums.UnitOfMeasure;
import com.market.banica.calculator.model.Product;
import com.market.banica.calculator.service.JMXServiceImpl;
import com.market.banica.calculator.service.contract.BackUpService;
import com.market.banica.calculator.service.contract.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class JMXServiceTests {

    @Mock
    private BackUpService backUpService;

    @Mock
    private ProductService productService;

    @Mock
    private ProductBase productBase;

    @InjectMocks
    private JMXServiceImpl jmxService;

    @Test
    public void createProduct_Should_createProduct_When_parametersAreValidAndProductIsNotComposite() {
        //given
        Product product = new Product();
        product.setProductName("productName");
        product.setUnitOfMeasure(UnitOfMeasure.GRAM);
        when(productService.doesProductExists(product.getProductName())).thenReturn(false);
        when(productService.createProduct(product.getProductName(),
                product.getUnitOfMeasure().toString(),"")).thenReturn(product);
        doNothing().when(productService).writeProductToDatabase(product.getProductName(), product);

        //when
        jmxService.createProduct(product.getProductName(), product.getUnitOfMeasure().toString(), "");

        //then
        verify(productService, times(1)).writeProductToDatabase(product.getProductName(), product);
        verify(productService, times(1)).createProduct(product.getProductName(),
                product.getUnitOfMeasure().toString(),"");
        verify(productService, times(1)).doesProductExists(product.getProductName());
        verifyNoMoreInteractions(productService, productBase);

    }

    @Test
    public void createProduct_Should_createProduct_When_parametersAreValidAndProductIsCompositeAndQuantitiesAreValid() {
        //given
        Product product = new Product();
        product.setProductName("productName");
        product.setUnitOfMeasure(UnitOfMeasure.GRAM);
        Map<String, Integer> ingredients = new HashMap<>();
        ingredients.put("kori",50);
        ingredients.put("water",120);
        product.setIngredients(ingredients);
        String ingredientsMap = "kori:50,water:120";
        when(productService.doesProductExists(product.getProductName())).thenReturn(false);
        when(productService.createProduct(product.getProductName(),
                product.getUnitOfMeasure().toString(), "kori:50,water:120")).thenReturn(product);
        doNothing().when(productService).writeProductToDatabase(product.getProductName(), product);

        //when
        jmxService.createProduct(product.getProductName(), product.getUnitOfMeasure().toString(), ingredientsMap);

        //then
        verify(productService, times(1)).writeProductToDatabase(product.getProductName(), product);
        verify(productService, times(1)).doesProductExists(product.getProductName());
        verify(productService, times(1)).createProduct(
                product.getProductName(),product.getUnitOfMeasure().toString(), "kori:50,water:120");
        verifyNoMoreInteractions(productService, productBase);

    }
}
