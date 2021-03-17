package com.market.banica.calculator.service;

import com.market.banica.calculator.data.contract.RecipesBase;
import com.market.banica.calculator.dto.ProductDto;
import com.market.banica.calculator.model.Product;
import com.market.banica.calculator.service.contract.BackUpService;
import com.market.banica.calculator.service.contract.ProductService;
import io.micrometer.core.lang.Nullable;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
        LOGGER.debug("Product service impl: In createProduct method");

        validateParameterForNullAndEmpty(products);

        validateAllProductsInListAreNew(products);

        createProductsInDatabase(products);

        createBackUp();

        String recipeName = getRecipeName(products);

        LOGGER.debug("Recipe {} successfully created", recipeName);
        return recipesBase.getDatabase().get(recipeName);
    }

    @Override
    public List<ProductDto> getProduct(String productName, @Nullable String parentProductName) {
        LOGGER.debug("Product service impl: In getRecipe method");

        Product product = recipesBase.getDatabase().get(productName);

        validateProductExist(productName, product);

        ProductDto productDto = mapProductToProductDto(parentProductName, product);

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

    @Override
    public void createBackUp() {
        LOGGER.debug("Product service impl: In createBackUp method");

        backUpService.writeBackUp();
    }

    private void createProductsInDatabase(List<Product> products) {
        LOGGER.debug("Product service impl: In createProductsInDatabase private method");

        for(Product product: products){
            recipesBase.getDatabase().put(product.getProductName(),product);
        }
    }

    private void validateAllProductsInListAreNew(List<Product> products) {
        LOGGER.debug("Product service impl: In validateAllProductsInListAreNew private method");

        for(Product newProduct: products){
            if(recipesBase.getDatabase().get(newProduct.getProductName())!= null){

                LOGGER.error("Product with name {} already exists",newProduct.getProductName());
                throw new IllegalArgumentException("Product already exists");
            }
        }
    }

    private String getRecipeName(List<Product> products) {
        LOGGER.debug("Product service impl: In getRecipeName private method");

        return products.get(0).getProductName();
    }

    private void validateProductExist(String productName, Product product) {
        LOGGER.debug("Product service impl: In validateProductExist private method");

        if (product == null) {

            LOGGER.error("Product with name {} does not exist", productName);
            throw new IllegalArgumentException("Product with this name does not exist");
        }
    }

    private void groupAllIngredientsFromRecipeInResultListAsProductDtos(List<ProductDto> result, Product recipe) {
        LOGGER.debug("Product service impl: In groupAllIngredientsFromRecipeInResultListAsProductDtos private method");

        Queue<Product> tempContainer = convertListOfProductNamesInArrayDequeOfProducts(recipe);
        Queue<String> tempParentProductNames = new ArrayDeque<>();

        tempParentProductNames.add(recipe.getProductName());

        while (!tempContainer.isEmpty()) {

            Product tempProduct = tempContainer.remove();
            String parentProductName = tempParentProductNames.poll();

            if (tempProduct.getIngredients().size() != 0) {

                Queue<Product> tempIngredientsQueue =convertListOfProductNamesInArrayDequeOfProducts(tempProduct);
                tempContainer.addAll(tempIngredientsQueue);
                result.addAll(mapQueueOfProductsToListOfProductDtos(
                        parentProductName, tempIngredientsQueue));
                tempParentProductNames.addAll(tempProduct.getIngredients());

            } else {

                result.add(mapProductToProductDto(parentProductName, tempProduct));
            }
        }
    }

    private List<ProductDto> mapQueueOfProductsToListOfProductDtos(String parentProductName, Queue<Product> tempIngredientsQueue) {
        LOGGER.debug("Product service impl: In mapQueueOfProductsToListOfProductDtos private method");

        List<ProductDto>productDtos = new ArrayList<>();

        for(Product product: tempIngredientsQueue){

            productDtos.add(mapProductToProductDto(parentProductName,product));
        }

        return productDtos;
    }

    private Queue<Product> convertListOfProductNamesInArrayDequeOfProducts(Product recipe) {
        LOGGER.debug("Product service impl: In convertListOfProductNamesInArrayDequeOfProducts private method");

        return recipe.getIngredients().stream()
                .map(productName -> recipesBase.getDatabase().get(productName))
                .collect(Collectors.toCollection(ArrayDeque::new));
    }

    // TODO matter of future update with Model Mapper, once it is merged with main, to avoid possible merge conflicts
    private ProductDto mapProductToProductDto(String parentRecipeName, Product recipe) {
        LOGGER.debug("Product service impl: In mapProductToProductDto private method");

        ProductDto productDto = new ProductDto();
        productDto.setProductName(recipe.getProductName());
        productDto.setUnitOfMeasure(recipe.getUnitOfMeasure());

        if(parentRecipeName != null) {

            if (doesIngredientBelongToRecipe(parentRecipeName, recipe)) {

                setQuantityPerParentWithParentForThisRecipe(parentRecipeName, recipe, productDto);

            } else {

                throwExceptionWhenProductDoesNotBelongToRecipe(parentRecipeName, recipe.getProductName());
            }
        }

        return productDto;
    }

    private void setQuantityPerParentWithParentForThisRecipe(String parentRecipeName, Product recipe, ProductDto productDto) {
        LOGGER.debug("Product service impl: In setQuantityPerParentWithParentForThisRecipe private method");

        productDto.getQuantityPerParent().put(parentRecipeName, recipe.getQuantityPerParent().get(parentRecipeName));
    }

    private void throwExceptionWhenProductDoesNotBelongToRecipe(String recipeName, String ingredientName) {
        LOGGER.debug("Product service impl: In throwExceptionWhenProductDoesNotBelongToRecipe private method");

        LOGGER.error("Ingredient {} does not belong to recipe {}", ingredientName, recipeName);
        throw new IllegalArgumentException("Ingredient does not belong to the recipe");
    }

    private boolean doesIngredientBelongToRecipe(String recipeName, Product ingredient) {
        LOGGER.debug("Product service impl: In doesIngredientBelongToRecipe private method");

        return ingredient.getQuantityPerParent().get(recipeName) != null;
    }

    private void validateParameterForNullAndEmpty(List<Product> products) {
        LOGGER.debug("Product service impl: In validateParameterForNullAndEmpty private method");

        if (products == null || products.isEmpty()) {

            LOGGER.error("Parameter {} passed to createRecipe is null or empty", products);
            throw new IllegalArgumentException("Recipes should be present to create recipe");
        }
    }

}
