package com.market.banica.calculator.service;

import com.market.banica.calculator.data.contract.RecipesBase;
import com.market.banica.calculator.dto.ProductDto;
import com.market.banica.calculator.model.Product;
import com.market.banica.calculator.service.contract.BackUpService;
import com.market.banica.calculator.service.contract.ProductService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductServiceImpl.class);

    private final RecipesBase recipesBase;
    private final BackUpService backUpService;

    @Override
    public Product createProduct(List<Product> products) {
        LOGGER.debug("In createProduct method with parameters: products {}", products);

        validateParameterForNullAndEmpty(products);

        validateAllProductsInListAreNew(products);

        createProductsInDatabase(products);

        backUpService.writeBackUp();

        String recipeName = getRecipeName(products);

        LOGGER.debug("Recipe {} successfully created", recipeName);
        return recipesBase.getDatabase().get(recipeName);
    }

    @Override
    public List<ProductDto> getProduct(String productName, @Nullable String parentProductName) {
        LOGGER.debug("In getProduct method with parameters:productName {} and  parentProductName {}",productName,parentProductName);

        Product product = getProductFromDatabase(productName);

        validateProductExist(productName, product);

        ProductDto productDto = mapProductToProductDto( product);

        List<ProductDto> result = new ArrayList<>();
        result.add(productDto);

        if (product.getIngredients().size() != 0) {
            groupAllIngredientsFromRecipeInResultListAsProductDtos(result, product);
        }

        LOGGER.debug("GetProduct with product name {} successfully invoked", productName);
        return result;
    }

    //TODO to be implemented once expectations are clear
    @Override
    public void getAllProducts(){}

    private Product getProductFromDatabase(String productName) {
        LOGGER.debug("In getProductFromDatabase method");

        return recipesBase.getDatabase().get(productName);
    }

    private void createProductsInDatabase(List<Product> products) {
        LOGGER.debug("In createProductsInDatabase private method");

        for(Product product: products){
            recipesBase.getDatabase().put(product.getProductName(),product);
        }
    }

    private void validateAllProductsInListAreNew(List<Product> products) {
        LOGGER.debug("In validateAllProductsInListAreNew private method");

        for(Product newProduct: products){
            if(getProductFromDatabase(newProduct.getProductName())!= null){

                LOGGER.error("Product with name {} already exists",newProduct.getProductName());
                throw new IllegalArgumentException("Product already exists");
            }
        }
    }

    private String getRecipeName(List<Product> products) {
        LOGGER.debug("In getRecipeName private method");

        return products.get(0).getProductName();
    }

    private void validateProductExist(String productName, Product product) {
        LOGGER.debug("In validateProductExist private method");

        if (product == null) {

            LOGGER.error("Product with name {} does not exist", productName);
            throw new IllegalArgumentException("Product with this name does not exist");
        }
    }

    private void groupAllIngredientsFromRecipeInResultListAsProductDtos(List<ProductDto> result, Product recipe) {
        LOGGER.debug("In groupAllIngredientsFromRecipeInResultListAsProductDtos private method");

        Queue<Product> tempContainer = convertListOfProductNamesInArrayDequeOfProducts(recipe);

        while (!tempContainer.isEmpty()) {

            Product tempProduct = tempContainer.remove();

            if (tempProduct.getIngredients().size() != 0) {

                Queue<Product> tempIngredientsQueue =convertListOfProductNamesInArrayDequeOfProducts(tempProduct);
                tempContainer.addAll(tempIngredientsQueue);
                result.addAll(mapQueueOfProductsToListOfProductDtos(tempIngredientsQueue));

            } else {

                result.add(mapProductToProductDto( tempProduct));
            }
        }
    }

    private List<ProductDto> mapQueueOfProductsToListOfProductDtos(Queue<Product> tempIngredientsQueue) {
        LOGGER.debug("In mapQueueOfProductsToListOfProductDtos private method");

        List<ProductDto>productDtos = new ArrayList<>();

        for(Product product: tempIngredientsQueue){

            productDtos.add(mapProductToProductDto(product));
        }

        return productDtos;
    }

    private Queue<Product> convertListOfProductNamesInArrayDequeOfProducts(Product recipe) {
        LOGGER.debug("In convertListOfProductNamesInArrayDequeOfProducts private method");

        return recipe.getIngredients().keySet().stream()
                .map(this::getProductFromDatabase)
                .collect(Collectors.toCollection(ArrayDeque::new));
    }

    // TODO matter of future update with Model Mapper, once it is merged with main, to avoid possible merge conflicts
    private ProductDto mapProductToProductDto(Product recipe) {
        LOGGER.debug("In mapProductToProductDto private method");

        ProductDto productDto = new ProductDto();
        productDto.setProductName(recipe.getProductName());
        productDto.setUnitOfMeasure(recipe.getUnitOfMeasure());
        productDto.setIngredients(recipe.getIngredients());

        return productDto;
    }

    private void validateParameterForNullAndEmpty(List<Product> products) {
        LOGGER.debug("In validateParameterForNullAndEmpty private method");

        if (products == null || products.isEmpty()) {

            LOGGER.error("Parameter {} passed to createRecipe is null or empty", products);
            throw new IllegalArgumentException("Recipes should be present to create recipe");
        }
    }

}
