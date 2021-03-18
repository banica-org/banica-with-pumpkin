package com.market.banica.calculator.service;

import com.market.banica.calculator.data.contract.RecipesBase;
import com.market.banica.calculator.enums.UnitOfMeasure;
import com.market.banica.calculator.model.Product;
import com.market.banica.calculator.service.contract.JMXService;
import com.market.banica.calculator.service.contract.ProductService;
import io.micrometer.core.lang.Nullable;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@EnableMBeanExport
@ManagedResource
@Service
@RequiredArgsConstructor
public class JMXServiceImpl implements JMXService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JMXServiceImpl.class);

    private final RecipesBase recipesBase;
    private final ProductService productService;

    @Override
    @ManagedOperation
    public Map<String, Product> getDatabase() {
        LOGGER.debug("JMX server impl: In getDatabase method");
        LOGGER.info("GetDatabase called from JMX server");

        return recipesBase.getDatabase();
    }

    @Override
    @ManagedOperation
    public void createProduct(String newProductName, String unitOfMeasure, Map<String, Integer> ingredients) {
        LOGGER.debug("JMX server impl: In createProduct method");
        LOGGER.info("CreateProduct called from JMX server with parameters newProductName {},unitOfMeasure {}," +
                        " ingredients {} and ingredients {}", newProductName, unitOfMeasure,
                ingredients, ingredients);
        Optional<Product> product = Optional.ofNullable(getDatabase().get(newProductName));

        if (product.isPresent()) {

            LOGGER.error("Product with name {} already exists", newProductName);
            throw new IllegalArgumentException("Product with this name already exists");
        } else {
            createNewProduct(newProductName, unitOfMeasure, ingredients);

            LOGGER.info("New product created from JMX server with product name {} and unit of measure {}"
                    , newProductName, unitOfMeasure);

            productService.createBackUp();
        }
    }

    @Override
    @ManagedOperation
    public void addIngredient(String recipeName, String ingredientName, int quantity) {
        LOGGER.debug("JMX server impl: In addIngredient method");
        LOGGER.info("AddIngredient called from JMX server with parameters recipeName {},ingredientName {} and quantityAsString {}",
                recipeName, ingredientName, quantity);

        retrieveProductFromDatabase(ingredientName);
        Product recipe = retrieveProductFromDatabase(recipeName);

        recipe.getIngredients().put(ingredientName, quantity);

        productService.createBackUp();

        LOGGER.info("Ingredient added from JMX server for recipe {} and ingredient {} with value {}"
                , recipeName, ingredientName, quantity);
    }

    @Override
    @ManagedOperation
    public void setProductQuantity(String recipeName, String ingredientName, int newQuantity) {
        LOGGER.debug("JMX server impl: In setProductQuantity method");
        LOGGER.info("SetProductQuantity called from JMX server with parameters recipeName {},ingredientName {}" +
                " and newValue {}", recipeName, ingredientName, newQuantity);

        Product parentProduct = retrieveProductFromDatabase(recipeName);

        if (doesIngredientBelongToRecipe(ingredientName, parentProduct)) {

            setProductQuantity(recipeName, parentProduct, newQuantity);

            productService.createBackUp();
        } else {

            throwExceptionWhenProductDoesNotBelongToRecipe(recipeName, ingredientName);
        }

        LOGGER.info("Value set from JMX server for recipe {} and ingredient {} with value {}"
                , recipeName, ingredientName, newQuantity);
    }

    @Override
    @ManagedOperation
    public int getProductQuantity(String recipeName, String ingredientName) {
        LOGGER.debug("JMX server impl: In getProductQuantity method");
        LOGGER.info("GetProductQuantity called from JMX server for recipe {} and ingredient {}", recipeName, ingredientName);

        Product parentProduct = retrieveProductFromDatabase(recipeName);

        if (doesIngredientBelongToRecipe(ingredientName, parentProduct)) {

            LOGGER.info("Value checked from JMX server for recipe {} and ingredient {}", recipeName, ingredientName);
            return getProductQuantity(ingredientName, parentProduct);

        } else {

            LOGGER.error("Ingredient {} does not belong to recipe {}", ingredientName, recipeName);
            throw new IllegalArgumentException("Ingredient does not belong to the recipe");
        }
    }

    @Override
    @ManagedOperation
    public String getUnitOfMeasure(String productName) {
        LOGGER.debug("JMX server impl: In getUnitOfMeasure method");
        LOGGER.info("GetUnitOfMeasure called from JMX server for product with name: {}", productName);

        Product product = retrieveProductFromDatabase(productName);

        LOGGER.info("UnitOfMeasure checked from JMX server for product with name {}", productName);
        return product.getUnitOfMeasure().toString();
    }

    @Override
    @ManagedOperation
    public void setUnitOfMeasure(String productName, String unitOfMeasure) {
        LOGGER.debug("JMX server impl: In setUnitOfMeasure method");
        LOGGER.info("SetUnitOfMeasure called from JMX server for product with name: {} " +
                "and new UnitOfMeasure: {}", productName, unitOfMeasure);

        Product product = retrieveProductFromDatabase(productName);

        product.setUnitOfMeasure(UnitOfMeasure.valueOf(unitOfMeasure.toUpperCase(Locale.ROOT)));

        productService.createBackUp();

        LOGGER.info("UnitOfMeasure set from JMX server for product with name {}" +
                " and new unitOfMeasure {}", productName, unitOfMeasure);
    }

    @Override
    @ManagedOperation
    public void deleteProduct(@Nullable String parentProductName, String productName) {
        LOGGER.debug("JMX server impl: In deleteIngredient method");
        LOGGER.info("DeleteIngredient called from JMX server for recipe {} and ingredient {}", parentProductName, productName);

        Product product = retrieveProductFromDatabase(productName);
        Product parentProduct = retrieveProductFromDatabase(parentProductName);

        if (parentProductName == null) {

            product.setDeleted(true);

        } else {

            if (doesIngredientBelongToRecipe(parentProductName, product)) {

                deleteParentIngredientRelationFromParentIngredients(parentProduct, product);

            } else {

                throwExceptionWhenProductDoesNotBelongToRecipe(parentProductName, productName);
            }
        }

        productService.createBackUp();

        LOGGER.info("Product deleted from JMX server for recipe {} and ingredient {}"
                , parentProductName, productName);
    }

    private Product retrieveProductFromDatabase(String productName) {
        LOGGER.debug("JMX server impl: In retrieveProductFromDatabase private method");

       Optional<Product> product = Optional.ofNullable(recipesBase.getDatabase().get(productName));

       return validateProductExist(productName,product);
    }

    private void deleteParentIngredientRelationFromParentIngredients(Product parentProduct, Product ingredient) {
        LOGGER.debug("JMX server impl: In deleteParentIngredientRelationFromQuantityPerParent private method");

        parentProduct.getIngredients().remove(ingredient.getProductName());
    }

    private String throwExceptionWhenProductDoesNotBelongToRecipe(String recipeName, String ingredientName) {
        LOGGER.debug("JMX server impl: In throwExceptionWhenProductDoesNotBelongToRecipe private method");

        LOGGER.error("Ingredient {} does not belong to recipe {}", ingredientName, recipeName);
        throw new IllegalArgumentException("Ingredient does not belong to the recipe");
    }

    private int getProductQuantity(String ingredientName, Product parentProduct) {
        LOGGER.debug("JMX server impl: In getProductQuantity private method");

        return parentProduct.getIngredients().get(ingredientName);
    }

    private void setProductQuantity(String product, Product parentProduct, int newQuantity) {
        LOGGER.debug("JMX server impl: In setProductQuantity private method");

        parentProduct.getIngredients().put(product, newQuantity);
    }

    private boolean doesIngredientBelongToRecipe(String productName, Product parentProduct) {
        LOGGER.debug("JMX server impl: In doesIngredientBelongToRecipe private method");

        return parentProduct.getIngredients().get(productName) != null;
    }

    private Product validateProductExist(String productName, Optional<Product> product) {
        LOGGER.debug("JMX server impl: In validateProductExist private method");

        if (product.isPresent()) {
            return product.get();
        }

        LOGGER.error("Product with name {} does not exist", productName);
        throw new IllegalArgumentException("Product with this name does not exist");

    }

    private void createNewProduct(String newProductName, String unitOfMeasure,
                                  Map<String, Integer> ingredients) {
        LOGGER.debug("JMX server impl: In createNewProduct private method");

        Product newRecipe = new Product();

        newRecipe.setProductName(newProductName);

        newRecipe.setUnitOfMeasure(UnitOfMeasure.valueOf(unitOfMeasure));

        if (!ingredients.isEmpty()) {
            validateProductsOfListExists(ingredients.keySet());
            newRecipe.setIngredients(ingredients);
        }
    }

    private void validateProductsOfListExists(Collection<String> productsNames) {
        LOGGER.debug("JMX server impl: In validateProductsOfListExists private method");

        for (String productName : productsNames) {

            retrieveProductFromDatabase(productName);
        }
    }
}
