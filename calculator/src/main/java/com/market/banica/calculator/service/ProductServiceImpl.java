package com.market.banica.calculator.service;

import com.market.banica.calculator.data.contract.ProductBase;
import com.market.banica.calculator.enums.UnitOfMeasure;
import com.market.banica.calculator.model.Product;
import com.market.banica.calculator.service.contract.BackUpService;
import com.market.banica.calculator.service.contract.ProductService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
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

    @Override
    public Product createProduct(List<Product> products) {
        LOGGER.debug("In createProduct method with parameters: products {}", products);

        validateParameterForNullAndEmpty(products);

        validateAllProductsInListAreNew(products);

        createProductsInDatabase(products);

//        backUpService.writeBackUp();

        String productName = getProductName(products);

        LOGGER.debug("Product {} successfully created", productName);
        return productBase.getDatabase().get(productName);
    }

    @Override
    public Product createProduct(String newProductName, String unitOfMeasure,
                                 String ingredientsMap) {
        LOGGER.debug("In createProduct method");

        Product newProduct = new Product();

        newProduct.setProductName(newProductName);

        newProduct.setUnitOfMeasure(UnitOfMeasure.valueOf(unitOfMeasure));

        Map<String, Integer> ingredients = new HashMap<>();

        if (!ingredientsMap.isEmpty()) {

            ingredients = setCompositeProductIngredients(ingredientsMap);
        }

        newProduct.setIngredients(ingredients);

        return newProduct;
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

    @Override
    public Product getProductFromDatabase(String productName) {
        LOGGER.debug("In getProductFromDatabase method");

        validateProductExists(productName);

        return productBase.getDatabase().get(productName);
    }

    @Override
    public void writeProductToDatabase(String newProductName, Product newProduct) {
        LOGGER.debug("In writeProductToDatabase method");

        productBase.getDatabase().put(newProductName, newProduct);
        backUpService.writeBackUp();
    }

    @Override
    public void validateProductsOfListExists(Collection<String> productsNames) {
        LOGGER.debug("In validateProductsOfListExists method");

        for (String productName : productsNames) {

            validateProductExists(productName);
        }
    }

    @Override
    public void validateProductExists(String productName) {
        LOGGER.debug("In validateProductExists method");

        if (!doesProductExists(productName)) {

            LOGGER.error("Product with name {} does not exists", productName);
            throw new IllegalArgumentException("Product with this name does not exists");
        }
    }

    @Override
    public boolean doesProductExists(String productName) {
        LOGGER.debug("In doesProductExists method");

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

}
