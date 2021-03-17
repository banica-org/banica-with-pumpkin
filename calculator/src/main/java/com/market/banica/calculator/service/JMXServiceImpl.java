package com.market.banica.calculator.service;

import com.market.banica.calculator.data.contract.RecipesBase;
import com.market.banica.calculator.enums.UnitOfMeasure;
import com.market.banica.calculator.model.Product;
import com.market.banica.calculator.service.contract.JMXService;
import com.market.banica.calculator.service.contract.ProductService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
    public Map<String,Product> getDatabase(){
      return   recipesBase.getDatabase();
    }

    @Override
    @ManagedOperation
    public void createProduct(String newProductName, String unitOfMeasure, Map<String, Integer> quantityPerParent, List<String> ingredients) {
        LOGGER.debug("JMX server impl: In createProduct method");
        LOGGER.info("CreateProduct called from JMX server with parameters newProductName {},unitOfMeasure {}," +
                        " quantityPerParent {} and ingredients {}", newProductName, unitOfMeasure,
                quantityPerParent, ingredients);
        Product product = recipesBase.getDatabase().get(newProductName);

        if (product == null) {
            createNewProduct(newProductName, unitOfMeasure, quantityPerParent, ingredients);

            LOGGER.info("New product created from JMX server with product name {} and unit of measure {}"
                    , newProductName, unitOfMeasure);

            productService.createBackUp();
        } else {

            LOGGER.error("Product with name {} already exists", newProductName);
            throw new IllegalArgumentException("Product with this name already exists");
        }
    }

    @Override
    @ManagedOperation
    public void addIngredient(String recipeName, String ingredientName, String quantityAsString) {
        LOGGER.debug("JMX server impl: In addIngredient method");
        LOGGER.info("AddIngredient called from JMX server with parameters recipeName {},ingredientName {} and quantityAsString {}",
                recipeName, ingredientName, quantityAsString);

        Product ingredient = retrieveProductFromDatabase(ingredientName);
        Product recipe = retrieveProductFromDatabase(recipeName);

        validateProductExist(ingredientName, ingredient);
        validateProductExist(recipeName, recipe);

        int quantity = getValueAsInt(quantityAsString);

        recipe.getIngredients().add(ingredientName);
        ingredient.getQuantityPerParent().put(recipeName, quantity);

        productService.createBackUp();

        LOGGER.info("Ingredient added from JMX server for recipe {} and ingredient {} with value {}"
                , recipeName, ingredientName, quantityAsString);
    }

    @Override
    @ManagedOperation
    public void setProductQuantity(String recipeName, String ingredientName, String newValue) {
        LOGGER.debug("JMX server impl: In setProductQuantity method");
        LOGGER.info("SetProductQuantity called from JMX server with parameters recipeName {},ingredientName {}" +
                " and newValue {}", recipeName, ingredientName, newValue);

        Product ingredient = retrieveProductFromDatabase(ingredientName);

        validateProductExist(ingredientName, ingredient);

        int newQuantity = getValueAsInt(newValue);

        if (doesIngredientBelongToRecipe(recipeName, ingredient)) {

            setProductQuantity(recipeName, ingredient, newQuantity);

            productService.createBackUp();
        } else {

            throwExceptionWhenProductDoesNotBelongToRecipe(recipeName, ingredientName);
        }

        LOGGER.info("Value set from JMX server for recipe {} and ingredient {} with value {}"
                , recipeName, ingredientName, newValue);
    }

    @Override
    @ManagedOperation
    public String getProductQuantity(String recipeName, String ingredientName) {
        LOGGER.debug("JMX server impl: In getProductQuantity method");
        LOGGER.info("GetProductQuantity called from JMX server for recipe {} and ingredient {}", recipeName, ingredientName);

        Product ingredient = retrieveProductFromDatabase(ingredientName);

        validateProductExist(ingredientName, ingredient);

        if (doesIngredientBelongToRecipe(recipeName, ingredient)) {

            LOGGER.info("Value checked from JMX server for recipe {} and ingredient {}", recipeName, ingredientName);
            return String.valueOf(getProductQuantity(recipeName, ingredient));

        } else {

            return throwExceptionWhenProductDoesNotBelongToRecipe(recipeName, ingredientName);
        }
    }

    @Override
    @ManagedOperation
    public String getUnitOfMeasure(String productName) {
        LOGGER.debug("JMX server impl: In getUnitOfMeasure method");
        LOGGER.info("GetUnitOfMeasure called from JMX server for product with name: {}",productName);

        Product product = retrieveProductFromDatabase(productName);

        validateProductExist(productName, product);

        LOGGER.info("UnitOfMeasure checked from JMX server for product with name {}", productName);
        return product.getUnitOfMeasure().toString();
    }

    @Override
    @ManagedOperation
    public void setUnitOfMeasure(String productName, String unitOfMeasure){
        LOGGER.debug("JMX server impl: In setUnitOfMeasure method");
        LOGGER.info("SetUnitOfMeasure called from JMX server for product with name: {} " +
                "and new UnitOfMeasure: {}",productName, unitOfMeasure);

        Product product = retrieveProductFromDatabase(productName);

        validateProductExist(productName, product);

        validateUnitOfMeasureExist(unitOfMeasure);

        product.setUnitOfMeasure(Enum.valueOf(UnitOfMeasure.class,
                unitOfMeasure.toUpperCase(Locale.ROOT)));

        productService.createBackUp();

        LOGGER.info("UnitOfMeasure set from JMX server for product with name {}" +
                " and new unitOfMeasure {}", productName,unitOfMeasure);
    }

    @Override
    @ManagedOperation
    public void deleteRecipe(String recipeName) {
        LOGGER.debug("JMX server impl: In deleteRecipe method");
        LOGGER.info("DeleteRecipe called from JMX server for product with name: {}",recipeName);

        Product recipe = retrieveProductFromDatabase(recipeName);

        validateProductExist(recipeName, recipe);

        recipe.setDeleted(true);

        productService.createBackUp();

        LOGGER.info("Recipe deleted from JMX server for recipe with name {}", recipeName);
    }

    @Override
    @ManagedOperation
    public void deleteIngredient(String recipeName, String ingredientName) {
        LOGGER.debug("JMX server impl: In deleteIngredient method");
        LOGGER.info("DeleteIngredient called from JMX server for recipe {} and ingredient {}", recipeName, ingredientName);

        Product ingredient = retrieveProductFromDatabase(ingredientName);

        validateProductExist(ingredientName, ingredient);

        if (isCompositeIngredient(ingredient)) {

            ingredient.setDeleted(true);

        } else {

            if (doesIngredientBelongToRecipe(recipeName, ingredient)) {

                deleteParentIngredientRelationFromQuantityPerParent(recipeName, ingredient);

            } else {

                throwExceptionWhenProductDoesNotBelongToRecipe(recipeName, ingredientName);
            }
        }

        productService.createBackUp();

        LOGGER.info("Ingredient deleted from JMX server for recipe {} and ingredient {}"
                , recipeName, ingredientName);
    }

    private void validateUnitOfMeasureExist(String unitOfMeasure) {
        LOGGER.debug("JMX server impl: In validateUnitOfMeasureExist private method");
        if(Arrays.stream(UnitOfMeasure.values())
                .anyMatch(unit->unit.name().equalsIgnoreCase(unitOfMeasure))){
            return;
        }

        LOGGER.error("Passed value {} is not a valid UnitOfMeasure enum",unitOfMeasure);
        throw new IllegalArgumentException("Value is not a valid UnitOfMeasure enum");
    }

    private Product retrieveProductFromDatabase(String productName) {
        LOGGER.debug("JMX server impl: In retrieveProductFromDatabase private method");

        return recipesBase.getDatabase().get(productName);
    }

    private void deleteParentIngredientRelationFromQuantityPerParent(String recipeName, Product ingredient) {
        LOGGER.debug("JMX server impl: In deleteParentIngredientRelationFromQuantityPerParent private method");

        ingredient.getQuantityPerParent().remove(recipeName);
    }

    private String throwExceptionWhenProductDoesNotBelongToRecipe(String recipeName, String ingredientName) {
        LOGGER.debug("JMX server impl: In throwExceptionWhenProductDoesNotBelongToRecipe private method");

        LOGGER.error("Ingredient {} does not belong to recipe {}", ingredientName, recipeName);
        throw new IllegalArgumentException("Ingredient does not belong to the recipe");
    }

    private int getProductQuantity(String recipeName, Product ingredient) {
        LOGGER.debug("JMX server impl: In getProductQuantity private method");

        return ingredient.getQuantityPerParent().get(recipeName);
    }

    private boolean isCompositeIngredient(Product ingredient) {
        LOGGER.debug("JMX server impl: In isCompositeIngredient private method");

        return ingredient.getIngredients().size() != 0;
    }

    private void setProductQuantity(String recipeName, Product ingredient, int newQuantity) {
        LOGGER.debug("JMX server impl: In setProductQuantity private method");

        ingredient.getQuantityPerParent().put(recipeName, newQuantity);
    }

    private boolean doesIngredientBelongToRecipe(String recipeName, Product ingredient) {
        LOGGER.debug("JMX server impl: In doesIngredientBelongToRecipe private method");

        return ingredient.getQuantityPerParent().get(recipeName) != null;
    }

    private int getValueAsInt(String newValue) {
        LOGGER.debug("JMX server impl: In getValueAsInt private method");

        try {
            return Integer.parseInt(newValue);
        } catch (NumberFormatException e) {
            LOGGER.error("New value passed to setValue is not convertible to int. Exception thrown");
        } catch (NullPointerException e) {
            LOGGER.error("New value passed to setValue is null. Exception thrown");
        }
        throw new IllegalArgumentException("Can not convert the new value to number");
    }

    private void validateProductExist(String productName, Product product) {
        LOGGER.debug("JMX server impl: In validateProductExist private method");

        if (product == null) {

            LOGGER.error("Product with name {} does not exist", productName);
            throw new IllegalArgumentException("Product with this name does not exist");
        }
    }

    private void createNewProduct(String newProductName, String unitOfMeasure,
                                  Map<String, Integer> quantityPerParent, List<String> ingredients) {
        LOGGER.debug("JMX server impl: In createNewProduct private method");

        Product newRecipe = new Product();

        newRecipe.setProductName(newProductName);

        validateUnitOfMeasureExist(unitOfMeasure);
        newRecipe.setUnitOfMeasure(Enum.valueOf(UnitOfMeasure.class, unitOfMeasure));

        if(!quantityPerParent.isEmpty()){
            validateProductsOfListExists(quantityPerParent.keySet());
            newRecipe.setQuantityPerParent(quantityPerParent);
        }
       if(!ingredients.isEmpty()){
           validateProductsOfListExists(ingredients);
           newRecipe.setIngredients(ingredients);
       }
    }

    private void validateProductsOfListExists(Collection<String> productsNames) {
        LOGGER.debug("JMX server impl: In validateProductsOfListExists private method");

        for(String productName:productsNames){

            validateProductExist(productName,retrieveProductFromDatabase(productName));
        }
    }
}
