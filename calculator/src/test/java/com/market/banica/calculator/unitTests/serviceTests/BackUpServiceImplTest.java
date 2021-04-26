package com.market.banica.calculator.unitTests.serviceTests;

import com.market.banica.calculator.data.contract.ProductBase;
import com.market.banica.calculator.enums.UnitOfMeasure;
import com.market.banica.calculator.model.Product;
import com.market.banica.calculator.service.BackUpServiceImpl;
import com.market.banica.common.util.ApplicationDirectoryUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BackUpServiceImplTest {

    public static final String PRODUCT_NAME = "crusts";
    public static final String BACK_UP_SERVICE_DATABASE_BACKUP_URL_FIELD = "databaseBackUpUrl";
    public static final String PRODUCT_DATABASE_BACKUP_URL_FIELD = "productBase";
    private final String DATABASE_BACKUP_URL = "backUpRecipeBase.json";
    private File testFile;
    private ConcurrentHashMap<String, Product> base;

    @Mock
    private ProductBase productBase;

    private BackUpServiceImpl backUpService;

    @BeforeEach
    void setUp() {
        backUpService = new BackUpServiceImpl(productBase);
        ReflectionTestUtils.setField(backUpService, BACK_UP_SERVICE_DATABASE_BACKUP_URL_FIELD, DATABASE_BACKUP_URL);
        base = new ConcurrentHashMap<>();
        base.put("crusts", createProduct());
    }

    @AfterEach
    void tearDown() throws IOException {
        ApplicationDirectoryUtil.getConfigFile(DATABASE_BACKUP_URL).delete();
    }

    @Test
    void readBackUpShouldCreateNewFileIfBackUpFileDoesNotExists() throws IOException {
        //Act
        backUpService.readBackUp();

        //Assert
        File jsonFile = ApplicationDirectoryUtil.getConfigFile(DATABASE_BACKUP_URL);
        assertEquals(jsonFile.length(), 0);
    }

    @Test
    void readBackUpShouldCreateAProductBaseBackUpFromBackUpFile() throws IOException {
        //Arrange
        createFileWithValidData();
        when(productBase.getDatabase()).thenReturn(base);

        //Act
        backUpService.readBackUp();

        //Assert
        ProductBase productBase = (ProductBase) ReflectionTestUtils.getField(backUpService, PRODUCT_DATABASE_BACKUP_URL_FIELD);

        Product product = productBase.getDatabase().get(PRODUCT_NAME);

        assertEquals(product.getProductName(), PRODUCT_NAME);
        assertEquals(product.getUnitOfMeasure(), UnitOfMeasure.GRAM);
    }

    private Product createProduct() {
        Product product = new Product();
        product.setProductName(PRODUCT_NAME);
        product.setUnitOfMeasure(UnitOfMeasure.GRAM);
        product.setIngredients(new HashMap<>());
        return product;
    }

    @Test
    void writeBackUpShouldWriteTheDataFromProductBaseToJsonFile() throws IOException {
        //Arrange
        createFileWithoutData();
        when(productBase.getDatabase()).thenReturn(base);

        //Act
        backUpService.writeBackUp();

        //Assert
        File file = ApplicationDirectoryUtil.getConfigFile(DATABASE_BACKUP_URL);
        assertTrue(file.length() != 0);
    }

    private void createFileWithoutData() throws IOException {
        testFile = ApplicationDirectoryUtil.getConfigFile(DATABASE_BACKUP_URL);

    }

    private void createFileWithValidData() throws IOException {
        testFile = ApplicationDirectoryUtil.getConfigFile(DATABASE_BACKUP_URL);

        String text = "{\n" +
                "  \"crusts\": {\n" +
                "    \"productName\": \"crusts\",\n" +
                "    \"unitOfMeasure\": \"GRAM\",\n" +
                "    \"ingredients\": {\n" +
                "      \"water\": 50,\n" +
                "      \"eggs\": 12\n" +
                "    }\n" +
                "  }\n" +
                "}";
        try (BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(testFile), StandardCharsets.UTF_8))) {
            bufferedWriter.write(text);
        }
    }
}