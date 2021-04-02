package com.market.banica.calculator.service;

import com.market.banica.calculator.data.contract.ProductBase;
import com.market.banica.calculator.enums.UnitOfMeasure;
import com.market.banica.calculator.model.Product;
import com.market.banica.calculator.service.contract.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JMXServiceImplTest {
    public static final String PRODUCT_NAME = "banica";
    public static final String INGREDIENT_NAME = "eggs";
    public static final Integer QUANTITY = 3;
    @Mock
    private ProductService productService;
    @Mock
    private ProductBase productBase;

    @InjectMocks
    private JMXServiceImpl jmxService;


    @Test
    void getDatabaseShouldReturnHasMapWithKeyProductNameAndValueProduct() {
        //Arrange
        HashMap<String, Product> productsDatabase = new HashMap<>();
        when(productBase.getDatabase()).thenReturn(productsDatabase);

        //Act
        Map<String, Product> database = jmxService.getDatabase();

        //Assert
        verify(productBase, times(1)).getDatabase();
        assertEquals(productsDatabase, database);

    }

    @Test
    void createProductShouldInvokeProductServiceCreateMethod() {
        //Act
        jmxService.createProduct(PRODUCT_NAME, UnitOfMeasure.GRAM.toString(), INGREDIENT_NAME);

        //Assert
        verify(productService, times(1)).createProduct(PRODUCT_NAME, UnitOfMeasure.GRAM.toString(), INGREDIENT_NAME);
    }

    @Test
    void addIngredientShouldInvokeProductServiceAddIngredientMethod() {
        //Act
        jmxService.addIngredient(PRODUCT_NAME, INGREDIENT_NAME, QUANTITY);

        //Assert
        verify(productService, times(1)).addIngredient(PRODUCT_NAME, INGREDIENT_NAME, QUANTITY);
    }

    @Test
    void setProductQuantityShouldInvokeProductServiceSetProductQuantityMethod() {
        //Act
        jmxService.setProductQuantity(PRODUCT_NAME, INGREDIENT_NAME, QUANTITY);

        //Assert
        verify(productService, times(1)).setProductQuantity(PRODUCT_NAME, INGREDIENT_NAME, QUANTITY);
    }

    @Test
    void getProductQuantityShouldInvokeProductServiceGetProductQuantityAndReturnProductQuantity() {
        //Arrange
        when(productService.getProductQuantity(PRODUCT_NAME, INGREDIENT_NAME)).thenReturn(1);
        //Act
        int expected = jmxService.getProductQuantity(PRODUCT_NAME, INGREDIENT_NAME);
        // Assert
        assertEquals(expected, 1);
    }

    @Test
    void getUnitOfMeasureShouldShouldInvokeProductServiceGetUnitOfMeasureMethodAndReturnProductUnitOfMeasure() {
        //Arrange
        when(productService.getUnitOfMeasure(PRODUCT_NAME)).thenReturn(UnitOfMeasure.GRAM.toString());

        //Act
        String expected = jmxService.getUnitOfMeasure(PRODUCT_NAME);
        // Assert
        assertEquals(expected, UnitOfMeasure.GRAM.toString());
    }

    @Test
    void setUnitOfMeasureShouldInvokeProductServiceSetUnityOfMeasureMethod() {
        //Act
        jmxService.setUnitOfMeasure(PRODUCT_NAME, UnitOfMeasure.GRAM.toString());

        //Assert
        verify(productService, times(1)).setUnitOfMeasure(PRODUCT_NAME, UnitOfMeasure.GRAM.toString());
    }

    @Test
    void deleteProductFromDatabaseShouldInvokeProductServiceDeleteProductFromDatabaseMethod() {
        //Act
        jmxService.deleteProductFromDatabase(PRODUCT_NAME);

        //Assert
        verify(productService, times(1)).deleteProductFromDatabase(PRODUCT_NAME);
    }

    @Test
    void deleteProductFromParentIngredientsShouldInvokeProductServiceDeleteProductFromParentIngredientsMethod() {
        //Act
        jmxService.deleteProductFromParentIngredients(PRODUCT_NAME, INGREDIENT_NAME);

        //Assert
        verify(productService, times(1)).deleteProductFromParentIngredients(PRODUCT_NAME, INGREDIENT_NAME);
    }
}