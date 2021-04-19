package com.market.banica.calculator.service;

import com.market.banica.calculator.data.contract.ProductBase;
import com.market.banica.calculator.enums.UnitOfMeasure;
import com.market.banica.calculator.model.Product;
import com.market.banica.calculator.service.contract.BackUpService;
import com.market.banica.calculator.service.contract.ProductService;
import com.market.banica.calculator.service.grpc.AuroraClientSideService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductServiceImpl.class);
    private static final String REGEX_DELIMITER_NEW_PRODUCT_INGREDIENTS = ",";
    private static final String REGEX_DELIMITER_NEW_PRODUCT_ENTRY_PAIRS = ":";

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

        if (doesProductExists(newProductName)) {

            LOGGER.error("Product with name {} already exists", newProductName);
            throw new IllegalArgumentException("Product with this name already exists");
        }

        Product newProduct = new Product();

        newProduct.setProductName(newProductName);

        newProduct.setUnitOfMeasure(UnitOfMeasure.valueOf(unitOfMeasure.toUpperCase(Locale.ROOT)));

        Map<String, Long> ingredients = new HashMap<>();

        if (!ingredientsMap.isEmpty()) {

            ingredients = setCompositeProductIngredients(ingredientsMap);
        }

        newProduct.setIngredients(ingredients);

        writeProductToDatabase(newProductName, newProduct);
    }

    @Override
    public void addIngredient(String parentProductName, String productName, long quantity) {
        LOGGER.debug("In addIngredient method with parameters: parentProductName {},productName {} and quantity {}" +
                parentProductName, productName, quantity);

        validateProductExists(productName);

        Product parentProduct = getProductFromDatabase(parentProductName);

        parentProduct.getIngredients().put(productName, quantity);

        writeProductToDatabase(parentProductName, parentProduct);
    }

    @Override
    public void setProductQuantity(String parentProductName, String productName, long newQuantity) {
        LOGGER.debug("In setProductQuantity method with parameters: parentProductName {},productName {}" +
                " and newQuantity {}", parentProductName, productName, newQuantity);

        Product parentProduct = getProductFromDatabase(parentProductName);

        validateProductBelongToParentProductIngredients(productName, parentProduct);

        parentProduct.getIngredients().put(productName, newQuantity);

        writeProductToDatabase(parentProductName, parentProduct);
    }

    @Override
    public long getProductQuantity(String parentProductName, String productName) {
        LOGGER.debug("In getProductQuantity method with parameters: parentProductName {} and productName {}"
                , parentProductName, productName);

        Product parentProduct = getProductFromDatabase(parentProductName);

        validateProductBelongToParentProductIngredients(productName, parentProduct);

        return parentProduct.getIngredients().get(productName);
    }

    @Override
    public String getUnitOfMeasure(String productName) {
        LOGGER.debug("In getUnitOfMeasure method with parameters: productName {}", productName);

        Product product = getProductFromDatabase(productName);

        return product.getUnitOfMeasure().toString();
    }

    @Override
    public void setUnitOfMeasure(String productName, String unitOfMeasure) {
        LOGGER.debug("In setUnitOfMeasure method with parameters: productName {} and unitOfMeasure {}", productName, unitOfMeasure);

        Product product = getProductFromDatabase(productName);

        product.setUnitOfMeasure(UnitOfMeasure.valueOf(unitOfMeasure.toUpperCase(Locale.ROOT)));

        writeProductToDatabase(productName, product);
    }

    @Override
    public void deleteProductFromDatabase(String productName) {
        LOGGER.debug("In deleteProductFromDatabase method with parameters: productName {}", productName);

        validateProductExists(productName);

        productBase.getDatabase().remove(productName);

        removeDeletedProductFromAllRecipes(productName);

        auroraClientSideService.cancelSubscription(productName);

        backUpService.writeBackUp();
    }

    @Override
    public void deleteProductFromParentIngredients(String parentProductName, String productName) {
        LOGGER.debug("In deleteIngredient method with parameters: parentProductName {} and productName {}", parentProductName, productName);

        Product parentProduct = getProductFromDatabase(parentProductName);

        validateProductBelongToParentProductIngredients(productName, parentProduct);

        parentProduct.getIngredients().remove(productName);

        writeProductToDatabase(parentProductName, parentProduct);
    }

    @Override
    public Map<Product, Map<String, Long>> getProductIngredientsWithQuantityPerParent(String productName) {
        LOGGER.debug("In getProductIngredientsWithQuantity method with parameters:productName {}"
                , productName);

        Product product = getProductFromDatabase(productName);

        Map<Product, Map<String, Long>> result = new HashMap<>();

        if (!product.getIngredients().isEmpty()) {
            addAllIngredientsFromProductToMapOfProductAndQuantity(result, product);
        }

        LOGGER.debug("GetProductIngredientsWithQuantity with product name {} successfully invoked", productName);
        return result;
    }

    @Override
    public Product getProductFromDatabase(String productName) {
        LOGGER.debug("In getProductFromDatabase method");

        validateProductExists(productName);

        return productBase.getDatabase().get(productName);
    }

    private void removeDeletedProductFromAllRecipes(String productName) {
        LOGGER.debug("In removeProductFromAllRecipes private method with parameters: productName {}", productName);

        productBase.getDatabase().forEach((key, value) -> value.getIngredients().remove(productName));
    }

    private void writeProductToDatabase(String newProductName, Product newProduct) {
        LOGGER.debug("In writeProductToDatabase private method");

//        announceInterestToOrderBookProductBase(newProductName);

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

    private Map<String, Long> setCompositeProductIngredients(String ingredientsMap) {
        LOGGER.debug("In setCompositeProductIngredients private method");

        Map<String, Long> ingredients = convertStringOfIngredientsToMap(ingredientsMap);

        validateProductsOfListExists(ingredients.keySet());

        return ingredients;
    }

    private Map<String, Long> convertStringOfIngredientsToMap(String ingredientsMap) {
        LOGGER.debug("In convertStringOfIngredientsToMap private method");

        Map<String, Long> ingredients = new HashMap<>();
        String[] ingredientsAsArray = ingredientsMap.split(REGEX_DELIMITER_NEW_PRODUCT_INGREDIENTS);

        for (String s : ingredientsAsArray) {

            String[] mapEntry = s.split(REGEX_DELIMITER_NEW_PRODUCT_ENTRY_PAIRS);
            long quantity = getValueAsInt(mapEntry[1]);
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

    private void addAllIngredientsFromProductToMapOfProductAndQuantity(Map<Product, Map<String, Long>> productQuantitiesMap,
                                                                       Product parentProduct) {
        LOGGER.debug("In addAllIngredientsFromProductToMapOfProductAndQuantity private method");

        Queue<Product> tempContainer = new ArrayDeque<>();
        tempContainer.add(parentProduct);

        while (!tempContainer.isEmpty()) {

            Product tempParentProduct = tempContainer.remove();

            if (!tempParentProduct.getIngredients().isEmpty()) {

                Collection<Product> tempIngredients =
                        convertProductIngredientsNamesToCollectionOfProducts(tempParentProduct);

                tempContainer.addAll(tempIngredients);

                tempIngredients.forEach(ingredient -> {
                    if (productQuantitiesMap.containsKey(ingredient)) {
                        productQuantitiesMap
                                .get(ingredient).put(tempParentProduct.getProductName()
                                , tempParentProduct.getIngredients().get(ingredient.getProductName()));
                    } else {
                        productQuantitiesMap.put(ingredient, new HashMap<String, Long>() {{
                            put(tempParentProduct.getProductName()
                                    , tempParentProduct.getIngredients().get(ingredient.getProductName()));
                        }});
                    }
                });
            }
        }
    }

    private Collection<Product> convertProductIngredientsNamesToCollectionOfProducts(Product parent) {
        LOGGER.debug("In convertProductIngredientsNamesToCollectionOfProducts private method");

        return parent.getIngredients().keySet().stream()
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
