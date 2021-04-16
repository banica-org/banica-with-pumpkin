package com.market.banica.calculator.service;

import com.market.banica.calculator.data.contract.ProductBase;
import com.market.banica.calculator.enums.UnitOfMeasure;
import com.market.banica.calculator.model.Product;
import com.market.banica.calculator.service.contract.BackUpService;
import com.market.banica.calculator.service.grpc.AuroraClientSideService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {
    public static final String INGREDIENTS_MAP = "pumpkin:2";
    public static final long QUANTITY = 2;
    private static final String BANICA = "banica";
    private static final String PUMPKIN = "pumpkin";
    private Product banica;
    private Product pumpkin;

    private Map<String, Product> demoDataBase;

    @Mock
    private BackUpService backUpService;
    @Mock
    private ProductBase productBase;
    @Mock
    private AuroraClientSideService auroraClientSideService;

    private ProductServiceImpl productService;

    @BeforeEach
    void setUp() {
        productService = new ProductServiceImpl(backUpService, productBase, auroraClientSideService);
        banica = createProduct(BANICA);
        pumpkin = createProduct(PUMPKIN);
        demoDataBase = createDataBase();
    }

    private Product createProduct(String productName) {
        Product product = new Product();
        product.setProductName(productName);
        product.setUnitOfMeasure(UnitOfMeasure.GRAM);
        product.setIngredients(new HashMap<>());
        return product;
    }

    private Map<String, Product> createDataBase() {
        Map<String, Product> database = new ConcurrentHashMap<>();
        database.put(BANICA, banica);
        return database;
    }

    @Test
    void firstCreateProductMethodShouldThrowIllegalArgumentExceptionIfListOfProductsIsNullOrEmpty() {
        //Act //Assert
        assertThrows(IllegalArgumentException.class, () -> productService.createProduct(new ArrayList<>()));
    }

    @Test
    void firstCreateProductMethodShouldThrowIllegalArgumentExceptionIfProductAlreadyExist() {
        //Arrange
        when(productBase.getDatabase()).thenReturn(demoDataBase);
        //Act //Assert
        assertThrows(IllegalArgumentException.class, () -> productService.createProduct(Arrays.asList(banica)));
    }

    @Test
    void firstCreateProductMethodShouldCreateNewProductIfItDoesNotExistAndAddItToProductBase() {
        //Arrange
        when(productBase.getDatabase()).thenReturn(demoDataBase);

        //Act
        Product product = productService.createProduct(Arrays.asList(pumpkin));

        //Assert
        verify(backUpService, times(1)).writeBackUp();
        assertEquals(PUMPKIN, product.getProductName());
    }

    @Test
    void secondCreateProductMethodShouldThrowIllegalArgumentExceptionIfProductAlreadyExist() {
        //Arrange
        when(productBase.getDatabase()).thenReturn(demoDataBase);

        //Assert
        assertThrows(IllegalArgumentException.class, () -> productService.createProduct(BANICA, UnitOfMeasure.GRAM.toString(), INGREDIENTS_MAP));
    }

    @Test
    void secondCreateProductMethodShouldCreateAProductAndSetIngredientsToIt() {
        //Arrange

        when(productBase.getDatabase()).thenReturn(getDatabaseWithIngredients());

        //Act
        productService.createProduct(BANICA, UnitOfMeasure.GRAM.toString(), INGREDIENTS_MAP);

        //Assert
        verify(backUpService, times(1)).writeBackUp();
    }

    @Test
    void addIngredientShouldThrowIllegalArgumentExceptionIfProductDoesNotExist() {
        //Arrange
        when(productBase.getDatabase()).thenReturn(new ConcurrentHashMap<>());

        //Act //Assert
        assertThrows(IllegalArgumentException.class, () -> productService.addIngredient(BANICA, PUMPKIN, 2));

    }

    @Test
    void addIngredientShouldAddIngredientToAppropriate() {
        //Arrange
        demoDataBase.put(PUMPKIN, pumpkin);
        when(productBase.getDatabase()).thenReturn(demoDataBase);

        //Act
        productService.addIngredient(BANICA, PUMPKIN, QUANTITY);

        //Assert
        verify(backUpService, times(1)).writeBackUp();
        verifyNoMoreInteractions(backUpService, backUpService, auroraClientSideService);
    }

    @Test
    void setProductQuantityShouldThrowIllegalArgumentExceptionIfParentProductDoesNotExist() {
        //Arrange
        when(productBase.getDatabase()).thenReturn(new ConcurrentHashMap<>());

        //Act
        assertThrows(IllegalArgumentException.class, () -> productService.setProductQuantity(BANICA, PUMPKIN, QUANTITY));
    }

    @Test
    void setProductQuantityShouldThrowIllegalArgumentExceptionIfParentProductDoesNotContainTheIngredient() {
        //Arrange
        demoDataBase.put(PUMPKIN, pumpkin);
        when(productBase.getDatabase()).thenReturn(demoDataBase);

        //Act
        assertThrows(IllegalArgumentException.class, () -> productService.setProductQuantity(BANICA, PUMPKIN, QUANTITY));
    }

    @Test
    void setProductQuantityShouldChangeIngredientQuantityIfTheBothProductsExists() {
        //Arrange
        setBanicaIngredients();
        when(productBase.getDatabase()).thenReturn(demoDataBase);

        //Act
        productService.setProductQuantity(BANICA, PUMPKIN, QUANTITY);

        //Assert
        verify(backUpService, times(1)).writeBackUp();
        verifyNoMoreInteractions(backUpService, backUpService, auroraClientSideService);
    }

    @Test
    void getProductQuantityShouldThrowIllegalArgumentExceptionIfParentProductDoesNotExist() {
        //Arrange
        when(productBase.getDatabase()).thenReturn(new ConcurrentHashMap<>());

        //Act
        assertThrows(IllegalArgumentException.class, () -> productService.getProductQuantity(BANICA, PUMPKIN));
    }

    @Test
    void getProductQuantityShouldThrowIllegalArgumentExceptionIfParentProductDoesNotContainIngredient() {
        //Arrange

        when(productBase.getDatabase()).thenReturn(demoDataBase);

        //Act
        assertThrows(IllegalArgumentException.class, () -> productService.getProductQuantity(BANICA, PUMPKIN));
    }

    @Test
    void getProductQuantityShouldIngredientQuantityFromAppropriateProduct() {
        //Arrange
        setBanicaIngredients();
        when(productBase.getDatabase()).thenReturn(demoDataBase);

        //Act
        long ingredientQuantity = productService.getProductQuantity(BANICA, PUMPKIN);

        assertEquals(ingredientQuantity, QUANTITY);
    }

    @Test
    void getUnitOfMeasureShouldThrowExceptionIfProductDoesNotExist() {
        //Arrange
        when(productBase.getDatabase()).thenReturn(new ConcurrentHashMap<>());
        //Act//Assert
        assertThrows(IllegalArgumentException.class, () -> productService.getUnitOfMeasure(BANICA));
    }

    @Test
    void getUnitOfMeasureShouldReturnTheUnityOfMeasureToAppropriateProduct() {
        //Arrange
        when(productBase.getDatabase()).thenReturn(demoDataBase);
        //Act//Assert
        assertEquals(productService.getUnitOfMeasure(BANICA), UnitOfMeasure.GRAM.toString());
    }

    @Test
    void setUnitOfMeasureShouldThrowExceptionIfProductDoesNotExist() {
        //Arrange
        when(productBase.getDatabase()).thenReturn(new ConcurrentHashMap<>());
        //Act//Assert
        assertThrows(IllegalArgumentException.class, () -> productService.setUnitOfMeasure(BANICA, UnitOfMeasure.PIECE.toString()));
    }

    @Test
    void setUnitOfMeasureShouldChangeTheAppropriateProductUnityOfMeasureAndWriteItBackToDataBase() {
        //Arrange
        when(productBase.getDatabase()).thenReturn(demoDataBase);
        //Act
        productService.setUnitOfMeasure(BANICA, UnitOfMeasure.PIECE.toString());
        //Assert
        verify(backUpService, times(1)).writeBackUp();
        verifyNoMoreInteractions(backUpService, backUpService, auroraClientSideService);
    }


    @Test
    void deleteProductFromDatabaseShouldThrowIllegalArgumentExceptionIfProductDoesNotExist() {
        //Arrange
        when(productBase.getDatabase()).thenReturn(new ConcurrentHashMap<>());

        //Act
        assertThrows(IllegalArgumentException.class, () -> productService.deleteProductFromDatabase(BANICA));
    }

    @Test
    void deleteProductFromDatabaseShouldDeleteProductFromDatabase() {
        //Arrange
        when(productBase.getDatabase()).thenReturn(demoDataBase);

        //Act
        productService.deleteProductFromDatabase(BANICA);

        //Assert
        verify(auroraClientSideService, times(1)).cancelSubscription(BANICA);
        verify(backUpService, times(1)).writeBackUp();
    }

    @Test
    void deleteProductFromParentIngredientsShouldThrowIllegalArgumentExceptionIfParentProductDoesNotExist() {
        //Arrange
        when(productBase.getDatabase()).thenReturn(new ConcurrentHashMap<>());
        //Act//Assert
        assertThrows(IllegalArgumentException.class, () -> productService.deleteProductFromParentIngredients(BANICA, PUMPKIN));
    }

    @Test
    void deleteProductFromParentIngredientsShouldThrowIllegalArgumentExceptionIfParentProductDoesNotContainIngredient() {
        //Arrange
        when(productBase.getDatabase()).thenReturn(demoDataBase);
        //Act//Assert
        assertThrows(IllegalArgumentException.class, () -> productService.deleteProductFromParentIngredients(BANICA, PUMPKIN));
    }

    @Test
    void deleteProductFromParentIngredientsShouldRemoveIngredientFromParentProductIngredients() {
        //Arrange
        setBanicaIngredients();
        when(productBase.getDatabase()).thenReturn(demoDataBase);
        //Act
        productService.deleteProductFromParentIngredients(BANICA, PUMPKIN);
        //Assert
        verify(backUpService, times(1)).writeBackUp();
        verifyNoMoreInteractions(backUpService, backUpService, auroraClientSideService);
    }

    @Test
    void getProductAsListProductShouldReturnProductAndHisIngredients() {
        //Arrange
        setBanicaIngredients();
        when(productBase.getDatabase()).thenReturn(demoDataBase);

        //Act
        Map<Product,List<Long>> products = productService.getProductIngredientsWithQuantity(BANICA);

        //Assert
        assertTrue(products.containsKey(banica));
        assertTrue(products.containsKey(pumpkin));

    }

    @Test
    void getProductAsListProductShouldReturnSingleProductAndWithoutIngredients() {
        //Arrange
        when(productBase.getDatabase()).thenReturn(demoDataBase);

        //Act
        Map<Product,List<Long>> products = productService.getProductIngredientsWithQuantity(BANICA);

        //Assert
        assertTrue(products.containsKey(banica));

    }

    private void setBanicaIngredients() {
        demoDataBase.put(PUMPKIN, pumpkin);
        Map<String, Long> ingredients = new ConcurrentHashMap<>();
        ingredients.put(PUMPKIN, QUANTITY);
        banica.setIngredients(ingredients);
    }

    private Map<String, Product> getDatabaseWithIngredients() {
        Map<String, Product> ingredients = new ConcurrentHashMap<>();
        ingredients.put(PUMPKIN, createProduct(PUMPKIN));
        return ingredients;
    }
}