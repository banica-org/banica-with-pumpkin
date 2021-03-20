package com.market.banica.calculator.service;

import com.market.banica.calculator.data.contract.RecipesBase;
import com.market.banica.calculator.enums.UnitOfMeasure;
import com.market.banica.calculator.model.Product;
import com.market.banica.calculator.service.contract.BackUpService;
import com.market.banica.calculator.service.contract.JMXService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@EnableMBeanExport
@ManagedResource
@Service
@RequiredArgsConstructor
public class JMXServiceImpl implements JMXService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JMXServiceImpl.class);
    private static final String REGEX_DELIMITER_NEW_PRODUCT_INGREDIENTS = ",";
    private static final String REGEX_DELIMITER_NEW_PRODUCT_ENTRY_PAIRS = ":";
    private static final String KEY_PREFIX_FOR_DELETED_PRODUCT = "deleted_";

    private final RecipesBase recipesBase;
    private final BackUpService backUpService;

    @Override
    @ManagedOperation
    public Map<String, Product> getDatabase() {
        LOGGER.info("GetDatabase called from JMX server");

        return recipesBase.getDatabase();
    }

    @Override
    @ManagedOperation
    public void createProduct(String newProductName, String unitOfMeasure, String ingredientsList) {
        LOGGER.debug("in createProduct method with parameters: newProductName {},unitOfMeasure {}," +
                        " ingredientsList {}", newProductName, unitOfMeasure,
                ingredientsList);
        LOGGER.info("CreateProduct called from JMX server");

        if (doesProductExists(newProductName)) {

            LOGGER.error("Product with name {} already exists", newProductName);
            throw new IllegalArgumentException("Product with this name already exists");
        }

        Product newProduct = createNewProduct(newProductName, unitOfMeasure, ingredientsList);

        addProductToDatabase(newProductName, newProduct);

        LOGGER.debug("New product created from JMX server with product name {} and unit of measure {}"
                , newProductName, unitOfMeasure);

        backUpService.writeBackUp();
    }

    @Override
    @ManagedOperation
    public void addIngredient(String parentProductName, String productName, int quantity) {
        LOGGER.debug("In addIngredient method with parameters: recipeName {},ingredientName {} and quantity {}" +
                parentProductName, productName, quantity);
        LOGGER.info("AddIngredient called from JMX server");

        if (!doesProductExists(productName)) {

            LOGGER.error("Product with name {} does not exists", productName);
            throw new IllegalArgumentException("Product with this name does not exists");
        }

        Product recipe = retrieveProductFromDatabase(parentProductName);

        recipe.getIngredients().put(productName, quantity);

        backUpService.writeBackUp();

        LOGGER.debug("Ingredient added from JMX server for recipeName {} and ingredientName {} with quantity {}"
                , parentProductName, productName, quantity);
    }

    @Override
    @ManagedOperation
    public void setProductQuantity(String parentProductName, String productName, int newQuantity) {
        LOGGER.debug("In setProductQuantity method with parameters: recipeName {},ingredientName {}" +
                " and newQuantity {}", parentProductName, productName, newQuantity);
        LOGGER.info("SetProductQuantity called from JMX server");

        Product parentProduct = retrieveProductFromDatabase(parentProductName);

        validateIngredientBelongToRecipe(productName, parentProduct);

        setProductQuantity(productName, parentProduct, newQuantity);

        backUpService.writeBackUp();


        LOGGER.debug("Value set from JMX server for recipeName {} and ingredientName {} with newQuantity {}"
                , parentProductName, productName, newQuantity);
    }

    @Override
    @ManagedOperation
    public int getProductQuantity(String parentProductName, String productName) {
        LOGGER.debug("In getProductQuantity method with parameters: recipeName {} and ingredientName {}", parentProductName, productName);
        LOGGER.info("GetProductQuantity called from JMX server");

        Product parentProduct = retrieveProductFromDatabase(parentProductName);

        validateIngredientBelongToRecipe(productName, parentProduct);

        LOGGER.debug("Value checked from JMX server for recipeName {} and ingredientName {}", parentProductName, productName);
        return getProductQuantity(productName, parentProduct);
    }

    @Override
    @ManagedOperation
    public String getUnitOfMeasure(String productName) {
        LOGGER.debug("In getUnitOfMeasure method with parameters: productName {}", productName);
        LOGGER.info("GetUnitOfMeasure called from JMX server");

        Product product = retrieveProductFromDatabase(productName);

        LOGGER.debug("UnitOfMeasure checked from JMX server for product with name {}", productName);
        return product.getUnitOfMeasure().toString();
    }

    @Override
    @ManagedOperation
    public void setUnitOfMeasure(String productName, String unitOfMeasure) {
        LOGGER.debug("In setUnitOfMeasure method with parameters: productName {} and unitOfMeasure {}", productName, unitOfMeasure);
        LOGGER.info("SetUnitOfMeasure called from JMX server");

        Product product = retrieveProductFromDatabase(productName);

        product.setUnitOfMeasure(UnitOfMeasure.valueOf(unitOfMeasure.toUpperCase(Locale.ROOT)));

        backUpService.writeBackUp();

        LOGGER.debug("UnitOfMeasure set from JMX server for product with name {}" +
                " and new unitOfMeasure {}", productName, unitOfMeasure);
    }

    @Override
    @ManagedOperation
    public void deleteProduct(String parentProductName, String productName) {
        LOGGER.debug("In deleteIngredient method with parameters: parentProductName {} and productName {}", parentProductName, productName);
        LOGGER.info("DeleteIngredient called from JMX server");

        Product product = retrieveProductFromDatabase(productName);

        if (Objects.equals(parentProductName, "")) {

            getDatabase().remove(productName);

            addProductToDatabase(KEY_PREFIX_FOR_DELETED_PRODUCT + productName, product);

        } else {

            Product parentProduct = retrieveProductFromDatabase(parentProductName);

            validateIngredientBelongToRecipe(productName, parentProduct);

            deleteParentIngredientRelationFromParentIngredients(parentProduct, product);
        }

        backUpService.writeBackUp();

        LOGGER.debug("Product deleted from JMX server for parentProductName {} and productName {}"
                , parentProductName, productName);
    }

    private void validateIngredientBelongToRecipe(String productName, Product parentProduct) {
        LOGGER.debug("In validateIngredientBelongToRecipe private method");

        if (parentProduct.getIngredients().get(productName) == null) {

            LOGGER.error("Ingredient {} does not belong to recipe {}", productName, parentProduct.getProductName());
            throw new IllegalArgumentException("Ingredient does not belong to the recipe");
        }
    }

    private void addProductToDatabase(String newProductName, Product newProduct) {
        LOGGER.debug("In addProductToDatabase private method");

        getDatabase().put(newProductName, newProduct);
    }

    private Product retrieveProductFromDatabase(String productName) {
        LOGGER.debug("In retrieveProductFromDatabase private method");

        if (!doesProductExists(productName)) {

            LOGGER.error("Product with name {} does not exist", productName);
            throw new IllegalArgumentException("Product with this name does not exist");
        }

        return getDatabase().get(productName);
    }

    private void deleteParentIngredientRelationFromParentIngredients(Product parentProduct, Product product) {
        LOGGER.debug("In deleteParentIngredientRelationFromQuantityPerParent private method");

        parentProduct.getIngredients().remove(product.getProductName());
    }

    private int getProductQuantity(String ingredientName, Product parentProduct) {
        LOGGER.debug("In getProductQuantity private method");

        return parentProduct.getIngredients().get(ingredientName);
    }

    private void setProductQuantity(String productName, Product parentProduct, int newQuantity) {
        LOGGER.debug("In setProductQuantity private method");

        parentProduct.getIngredients().put(productName, newQuantity);
    }

    private boolean doesProductExists(String productName) {
        LOGGER.debug("In validateProductExist private method");

        return getDatabase().containsKey(productName);
    }

    private Product createNewProduct(String newProductName, String unitOfMeasure,
                                     String ingredientsList) {
        LOGGER.debug("In createNewProduct private method");

        Product newRecipe = new Product();

        newRecipe.setProductName(newProductName);

        newRecipe.setUnitOfMeasure(UnitOfMeasure.valueOf(unitOfMeasure));

        if (!ingredientsList.isEmpty()) {

            Map<String, Integer> ingredients = convertStringOfIngredientsToMap(ingredientsList);

            validateProductsOfListExists(ingredients.keySet());

            newRecipe.setIngredients(ingredients);
        }

        return newRecipe;
    }

    private Map<String, Integer> convertStringOfIngredientsToMap(String ingredientsList) {
        LOGGER.debug("In convertStringOfIngredientsToMap private method");

        Map<String, Integer> ingredients = new HashMap<>();
        String[] ingredientsAsArray = ingredientsList.split(REGEX_DELIMITER_NEW_PRODUCT_INGREDIENTS);

        for (String s : ingredientsAsArray) {

            String[] mapEntry = s.split(REGEX_DELIMITER_NEW_PRODUCT_ENTRY_PAIRS);
            int quantity = getValueAsInt(mapEntry[1]);
            ingredients.put(mapEntry[0], quantity);
        }

        return ingredients;
    }

    private int getValueAsInt(String quantity) {
        LOGGER.debug("In getValueAsInt private method");

        try {
            return Integer.parseInt(quantity);
        } catch (NumberFormatException e) {
            LOGGER.error("String passed is not convertible to int. String value: {}. Exception thrown", quantity);
        } catch (NullPointerException e) {
            LOGGER.error("String passed is null. Exception thrown");
        }
        throw new IllegalArgumentException("Can not convert the string to number");
    }

    private void validateProductsOfListExists(Collection<String> productsNames) {
        LOGGER.debug("In validateProductsOfListExists private method");

        for (String productName : productsNames) {

            retrieveProductFromDatabase(productName);
        }
    }
}
