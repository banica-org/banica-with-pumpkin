package com.market.banica.calculator.service;

import com.market.banica.calculator.data.contract.ProductBase;
import com.market.banica.calculator.enums.UnitOfMeasure;
import com.market.banica.calculator.model.Product;
import com.market.banica.calculator.service.contract.BackUpService;
import com.market.banica.calculator.service.contract.ProductService;
import com.market.banica.calculator.service.grpc.AuroraClientSideService;
import com.market.banica.common.validator.DataValidator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductServiceImpl.class);
    private static final String REGEX_DELIMITER_NEW_PRODUCT_INGREDIENTS = ",";
    private static final String REGEX_DELIMITER_NEW_PRODUCT_ENTRY_PAIRS = ":";
    private static final String REGEX_INGREDIENTS_PATTERN = "(.+[a-z])(:)(.+\\d)";

    private final BackUpService backUpService;
    private final ProductBase productBase;
    private final AuroraClientSideService auroraClientSideService;

    @Override
    public Product createProduct(List<Product> products) {
        LOGGER.debug("In createProduct method with parameters: products {}", products);

        validateParameterForNullAndEmpty(products);

        validateAllProductsInListAreNew(products);

        createProductsInDatabase(products);

        String productName = getProductName(products);

        LOGGER.debug("Product {} successfully created", productName);
        return productBase.getDatabase().get(productName);
    }

    @Override
    public void createProduct(String newProductName, String unitOfMeasure,
                              String ingredientsMap) {
        LOGGER.debug("In createProduct method with parameters: newProductName {}, unitOfMeasure {} and ingredientsMap {}"
                , newProductName, unitOfMeasure, ingredientsMap);

        DataValidator.validateIncomingData(newProductName);
        DataValidator.validateIncomingData(unitOfMeasure);

        if (doesProductExists(newProductName)) {

            LOGGER.error("Product with name {} already exists", newProductName);
            throw new IllegalArgumentException("Product with this name already exists");
        }

        Product newProduct = new Product();

        newProduct.setProductName(newProductName);

        newProduct.setUnitOfMeasure(UnitOfMeasure.valueOf(unitOfMeasure.toUpperCase(Locale.ROOT)));

        Map<String, Integer> ingredients = new HashMap<>();

        if (!ingredientsMap.isEmpty()) {

            ingredients = setCompositeProductIngredients(ingredientsMap);
        }

        newProduct.setIngredients(ingredients);

        writeProductToDatabase(newProductName, newProduct);
    }

    @Override
    public void addIngredient(String parentProductName, String productName, int quantity) {
        LOGGER.debug("In addIngredient method with parameters: parentProductName {},productName {} and quantity {}" +
                parentProductName, productName, quantity);

        DataValidator.validateIncomingData(parentProductName);
        DataValidator.validateIncomingData(productName);

        validateProductExists(productName);

        Product parentProduct = getProductFromDatabase(parentProductName);

        parentProduct.getIngredients().put(productName, quantity);

        writeProductToDatabase(parentProductName, parentProduct);
    }

    @Override
    public void setProductQuantity(String parentProductName, String productName, int newQuantity) {
        LOGGER.debug("In setProductQuantity method with parameters: parentProductName {},productName {}" +
                " and newQuantity {}", parentProductName, productName, newQuantity);

        DataValidator.validateIncomingData(productName);
        DataValidator.validateIncomingData(parentProductName);

        Product parentProduct = getProductFromDatabase(parentProductName);

        validateProductBelongToParentProductIngredients(productName, parentProduct);

        parentProduct.getIngredients().put(productName, newQuantity);

        writeProductToDatabase(parentProductName, parentProduct);
    }

    @Override
    public int getProductQuantity(String parentProductName, String productName) {
        LOGGER.debug("In getProductQuantity method with parameters: parentProductName {} and productName {}"
                , parentProductName, productName);

        DataValidator.validateIncomingData(productName);
        DataValidator.validateIncomingData(parentProductName);

        Product parentProduct = getProductFromDatabase(parentProductName);

        validateProductBelongToParentProductIngredients(productName, parentProduct);

        return parentProduct.getIngredients().get(productName);
    }

    @Override
    public String getUnitOfMeasure(String productName) {
        LOGGER.debug("In getUnitOfMeasure method with parameters: productName {}", productName);

        DataValidator.validateIncomingData(productName);

        Product product = getProductFromDatabase(productName);

        return product.getUnitOfMeasure().toString();
    }

    @Override
    public void setUnitOfMeasure(String productName, String unitOfMeasure) {
        LOGGER.debug("In setUnitOfMeasure method with parameters: productName {} and unitOfMeasure {}", productName, unitOfMeasure);

        DataValidator.validateIncomingData(productName);
        DataValidator.validateIncomingData(unitOfMeasure);

        Product product = getProductFromDatabase(productName);

        product.setUnitOfMeasure(UnitOfMeasure.valueOf(unitOfMeasure.toUpperCase(Locale.ROOT)));

        writeProductToDatabase(productName, product);
    }

    @Override
    public void deleteProductFromDatabase(String productName) {
        LOGGER.debug("In deleteProductFromDatabase method with parameters: productName {}", productName);

        DataValidator.validateIncomingData(productName);

        validateProductExists(productName);

        productBase.getDatabase().remove(productName);

        removeDeletedProductFromAllRecipes(productName);

        auroraClientSideService.cancelSubscription(productName);

        backUpService.writeBackUp();
    }

    @Override
    public void deleteProductFromParentIngredients(String parentProductName, String productName) {
        LOGGER.debug("In deleteIngredient method with parameters: parentProductName {} and productName {}", parentProductName, productName);

        DataValidator.validateIncomingData(productName);
        DataValidator.validateIncomingData(parentProductName);

        Product parentProduct = getProductFromDatabase(parentProductName);

        validateProductBelongToParentProductIngredients(productName, parentProduct);

        parentProduct.getIngredients().remove(productName);

        writeProductToDatabase(parentProductName, parentProduct);
    }

    @Override
    public List<Product> getProductAsListProduct(String productName) {
        LOGGER.debug("In getProductAsListProduct method with parameters:productName {}"
                , productName);

        Product product = getProductFromDatabase(productName);

        List<Product> result = new ArrayList<>();

        result.add(product);

        if (!product.getIngredients().isEmpty()) {
            addAllIngredientsFromProductInListAsProduct(result, product);
        }

        LOGGER.debug("GetProductAsListProduct with product name {} successfully invoked", productName);
        return result;
    }

    //TODO to be implemented once expectations are clear
    @Override
    public void getAllProductsAsListProduct() {
    }

    private void removeDeletedProductFromAllRecipes(String productName) {
        LOGGER.debug("In removeProductFromAllRecipes private method with parameters: productName {}", productName);

        productBase.getDatabase().forEach((key, value) -> value.getIngredients().remove(productName));
    }

    private Product getProductFromDatabase(String productName) {
        LOGGER.debug("In getProductFromDatabase private method");

        validateProductExists(productName);

        return productBase.getDatabase().get(productName);
    }

    private void writeProductToDatabase(String newProductName, Product newProduct) {
        LOGGER.debug("In writeProductToDatabase private method");

        announceInterestToOrderBookProductBase(newProductName);

        productBase.getDatabase().put(newProductName, newProduct);

        backUpService.writeBackUp();
    }

    private void announceInterestToOrderBookProductBase(String newProductName) {
        LOGGER.debug("In announceInterestToOrderBookProductBase private method");

        if (!doesProductExists(newProductName)) {
            auroraClientSideService.announceInterests(newProductName);
        }
    }

    private void validateProductsOfListExists(Collection<String> productsNames) {
        LOGGER.debug("In validateProductsOfListExists private method");

        for (String productName : productsNames) {

            validateProductExists(productName);
        }
    }

    private void validateProductExists(String productName) {
        LOGGER.debug("In validateProductExists private method");

        if (!doesProductExists(productName)) {

            LOGGER.error("Product with name {} does not exists", productName);
            throw new IllegalArgumentException("Product with this name does not exists");
        }
    }

    private boolean doesProductExists(String productName) {
        LOGGER.debug("In doesProductExists private method");

        return productBase.getDatabase().containsKey(productName);
    }

    private Map<String, Integer> setCompositeProductIngredients(String ingredientsMap) {
        LOGGER.debug("In setCompositeProductIngredients private method");

        Map<String, Integer> ingredients = convertStringOfIngredientsToMap(ingredientsMap);

        validateProductsOfListExists(ingredients.keySet());

        return ingredients;
    }

    private Map<String, Integer> convertStringOfIngredientsToMap(String ingredientsMap) {
        LOGGER.debug("In convertStringOfIngredientsToMap private method");

        Map<String, Integer> ingredients = new HashMap<>();

        DataValidator.validateIngredientsMap(ingredientsMap);

        String[] ingredientsAsArray = ingredientsMap.split(REGEX_DELIMITER_NEW_PRODUCT_INGREDIENTS);

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

    private void createProductsInDatabase(List<Product> products) {
        LOGGER.debug("In createProductsInDatabase private method");

        for (Product product : products) {
            writeProductToDatabase(product.getProductName(), product);
        }
    }

    private void validateAllProductsInListAreNew(List<Product> products) {
        LOGGER.debug("In validateAllProductsInListAreNew private method");

        for (Product newProduct : products) {
            if (doesProductExists(newProduct.getProductName())) {

                LOGGER.error("Product with name {} already exists", newProduct.getProductName());
                throw new IllegalArgumentException("Product already exists");
            }
        }
    }

    private String getProductName(List<Product> products) {
        LOGGER.debug("In getProductName private method");

        return products.get(0).getProductName();
    }

    private void addAllIngredientsFromProductInListAsProduct(List<Product> result, Product recipe) {
        LOGGER.debug("In addAllIngredientsFromProductInListAsProduct private method");

        Queue<Product> tempContainer = convertListOfProductNamesInQueueOfProducts(recipe);

        while (!tempContainer.isEmpty()) {

            Product tempProduct = tempContainer.remove();

            if (!tempProduct.getIngredients().isEmpty()) {

                Queue<Product> tempIngredientsQueue = convertListOfProductNamesInQueueOfProducts(tempProduct);

                tempContainer.addAll(tempIngredientsQueue);

                result.addAll(tempIngredientsQueue);

            } else {

                result.add(tempProduct);
            }
        }
    }

    private Queue<Product> convertListOfProductNamesInQueueOfProducts(Product recipe) {
        LOGGER.debug("In convertListOfProductNamesInQueueOfProducts private method");

        return recipe.getIngredients().keySet().stream()
                .map(this::getProductFromDatabase)
                .collect(Collectors.toCollection(ArrayDeque::new));
    }

    private void validateParameterForNullAndEmpty(List<Product> products) {
        LOGGER.debug("In validateParameterForNullAndEmpty private method");

        if (products == null || products.isEmpty()) {

            LOGGER.error("Parameter {} passed to createRecipe is null or empty", products);
            throw new IllegalArgumentException("List with products should be present to create product");
        }
    }

    private void validateProductBelongToParentProductIngredients(String productName, Product parentProduct) {
        LOGGER.debug("In validateProductBelongToParentProductIngredients private method");

        if (!parentProduct.getIngredients().containsKey(productName)) {

            LOGGER.error("Product {} does not belong to ingredients of parent product {}", productName, parentProduct.getProductName());
            throw new IllegalArgumentException("Product does not belong to ingredients of the parent product");
        }
    }
}
