package com.market.banica.calculator.service;

import com.market.banica.calculator.data.contract.ProductBase;
import com.market.banica.calculator.model.Product;
import com.market.banica.calculator.service.contract.JMXServiceMBean;
import com.market.banica.calculator.service.contract.ProductService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Service;

import java.util.Map;

@EnableMBeanExport
@ManagedResource
@Service
@RequiredArgsConstructor
public class JMXServiceImpl implements JMXServiceMBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(JMXServiceImpl.class);

    private final ProductService productService;
    private final ProductBase productBase;

    @Override
    @ManagedOperation
    public Map<String, Product> getDatabase() {
        LOGGER.info("GetDatabase called from JMX server");

        return productBase.getDatabase();
    }

    @Override
    @ManagedOperation
    public void createProduct(String newProductName, String unitOfMeasure, String ingredientsList) {
        LOGGER.debug("in createProduct method with parameters: newProductName {},unitOfMeasure {}," +
                        " ingredientsList {}", newProductName, unitOfMeasure,
                ingredientsList);
        LOGGER.info("CreateProduct called from JMX server");

        productService.createProduct(newProductName, unitOfMeasure, ingredientsList);

        LOGGER.debug("New product created from JMX server with product name {} and unit of measure {}"
                , newProductName, unitOfMeasure);
    }

    @Override
    @ManagedOperation
    public void addIngredient(String parentProductName, String productName, int quantity) {
        LOGGER.debug("In addIngredient method with parameters: parentProductName {},productName {} and quantity {}" +
                parentProductName, productName, quantity);
        LOGGER.info("AddIngredient called from JMX server");

        productService.addIngredient(parentProductName, productName, quantity);

        LOGGER.debug("Product {} added from JMX server to ingredients of parent product {} with quantity {}"
                , productName, parentProductName, quantity);
    }

    @Override
    @ManagedOperation
    public void setProductQuantity(String parentProductName, String productName, int newQuantity) {
        LOGGER.debug("In setProductQuantity method with parameters: parentProductName {},productName {}" +
                " and newQuantity {}", parentProductName, productName, newQuantity);
        LOGGER.info("SetProductQuantity called from JMX server");

        productService.setProductQuantity(parentProductName, productName, newQuantity);

        LOGGER.debug("Quantity set from JMX server to new quantity {} for product {} with parent product {}",
                newQuantity, parentProductName, productName);
    }

    @Override
    @ManagedOperation
    public int getProductQuantity(String parentProductName, String productName) {
        LOGGER.debug("In getProductQuantity method with parameters: parentProductName {} and productName {}"
                , parentProductName, productName);
        LOGGER.info("GetProductQuantity called from JMX server");


        int result = productService.getProductQuantity(parentProductName, productName);

        LOGGER.debug("Quantity checked from JMX server for product {} with parent product {}", parentProductName, productName);
        return result;
    }

    @Override
    @ManagedOperation
    public String getUnitOfMeasure(String productName) {
        LOGGER.debug("In getUnitOfMeasure method with parameters: productName {}", productName);
        LOGGER.info("GetUnitOfMeasure called from JMX server");


        String result = productService.getUnitOfMeasure(productName);

        LOGGER.debug("UnitOfMeasure checked from JMX server for product {}", productName);
        return result;
    }

    @Override
    @ManagedOperation
    public void setUnitOfMeasure(String productName, String unitOfMeasure) {
        LOGGER.debug("In setUnitOfMeasure method with parameters: productName {} and unitOfMeasure {}", productName, unitOfMeasure);
        LOGGER.info("SetUnitOfMeasure called from JMX server");

        productService.setUnitOfMeasure(productName, unitOfMeasure);

        LOGGER.debug("UnitOfMeasure set from JMX server for product {}" +
                " with new unitOfMeasure {}", productName, unitOfMeasure);
    }

    @Override
    @ManagedOperation
    public void deleteProductFromDatabase(String productName) {
        LOGGER.debug("In deleteProductFromDatabase method with parameters: productName {}", productName);
        LOGGER.info("DeleteProductFromDatabase called from JMX server");


        productService.deleteProductFromDatabase(productName);

        LOGGER.debug("Product {} deleted from JMX server", productName);
    }

    @Override
    @ManagedOperation
    public void deleteProductFromParentIngredients(String parentProductName, String productName) {
        LOGGER.debug("In deleteIngredient method with parameters: parentProductName {} and productName {}", parentProductName, productName);
        LOGGER.info("DeleteIngredient called from JMX server");

        productService.deleteProductFromParentIngredients(parentProductName, productName);

        LOGGER.debug("Product deleted from JMX server for parent product {} and product {}"
                , parentProductName, productName);
    }


}
