package com.market.banica.calculator.data;

import lombok.Getter;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

@Component
@Getter(onMethod_ = {@ManagedOperation})
@EnableMBeanExport
@ManagedResource
@ToString
public class RecipesBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipesBase.class);

    private Properties database = new Properties();

    public void setPropertyWithBackUp(String key, String value) {
        LOGGER.debug("Recipes base: In setPropertyWithBackUp method");
        getDatabase().setProperty(key, value);
        writeBackUp();
    }

    public Map<String, Map<String,String>> getAllRecipes() {
        LOGGER.debug("Recipes base: In getAllRecipes method");

        return getDatabase().entrySet().stream()
                .collect(Collectors.toMap(
                        entry->entry.getKey() +"",
                        entry->getRecipe((String) entry.getKey())));
    }

    public Map<String, String> getRecipe(String recipeName) {
        LOGGER.debug("Recipes base: In getRecipe method");

        String recipeContent = getDatabase().getProperty(recipeName);

        validateRecipeExist(recipeName, recipeContent);

        Map<String, String> result;
        String delimiterIngredients = ",";

        if (hasRecipeMoreThanOneIngredient(recipeContent,delimiterIngredients)) {
           result = convertArrayOfIngredientsToMap(recipeContent.split(delimiterIngredients));
        }else{
            result = convertArrayOfIngredientsToMap(new String[]{recipeContent});
        }

        return result;
    }

    @ManagedOperation
    public void setValue(String recipeName, String ingredientName, String newValue) {
        LOGGER.debug("Recipes base: In setValue method");
        LOGGER.info("SetValue called from JMX server with parameters recipeName {},ingredientName {} and newValue{}", recipeName, ingredientName, newValue);

        String recipe = database.getProperty(recipeName);
        if (recipe == null) {
            createNewRecipeWhenReceiptNotFound(recipeName, ingredientName, newValue);
            return;
        }

        String[] ingredients = recipe.split(",");
        boolean ingredientExists = checkIfIngredientExist(ingredientName, ingredients);


        String newRecipe;
        if (ingredientExists) {
            updateIngredients(ingredientName, newValue, ingredients, recipeName);
            newRecipe = createRecipeAsStringFromArray(ingredients);
            setPropertyWithBackUp(recipeName, newRecipe);
        } else {
            newRecipe = createRecipeAsStringFromArray(ingredients);
            createNewIngredientIfDoNotExist(recipeName, ingredientName, newValue, newRecipe);
        }
        LOGGER.info("Value set from JMX server for recipe {} and ingredient {} with value {}"
                , recipeName, ingredientName, newValue);
    }

    @ManagedOperation
    public String getValue(String recipeName, String ingredientName) {
        LOGGER.debug("Recipes base: In getValue method");
        LOGGER.info("GetValue called from JMX server for recipe {} and ingredient {}", recipeName, ingredientName);
        String result = "Ingredient not found";

        String recipe = database.getProperty(recipeName);

        if (checkProductDoNotExist(recipe, recipeName, ingredientName)) {
            return result;
        }

        result = getResultFromRecipeString(ingredientName, result, recipe);

        LOGGER.info("Value checked from JMX server for recipe {} and ingredient {}", recipeName, ingredientName);
        return result;
    }

    @PostConstruct
    public void readBackUp() {
        LOGGER.debug("Recipes base: In readBackUp method");

        try (InputStream input = new FileInputStream("calculator/target/" + getClass().getSimpleName())) {

            Properties props = getPropertiesFromBackUpFile(input);

            setDatabaseFromBackUp(props);

            LOGGER.info("Recipes database set from exterior file at location {}", "calculator/target/" + getClass().getSimpleName());
        } catch (IOException e) {
            LOGGER.error("Exception thrown during reading back-up at start up", e);
        }
    }

    public void writeBackUp() {
        LOGGER.debug("Recipes base: In readBackUp method");

        try (OutputStream output = new FileOutputStream("calculator/target/" + getClass()
                .getSimpleName())) {

            getDatabase().store(output, "Back-up for: ");

            LOGGER.info("Recipes database back-up created in exterior file at location {}", "calculator/target/" + getClass()
                    .getSimpleName());
        } catch (IOException e) {
            LOGGER.error("Exception thrown during writing back-up at start up for database file: {}", getDatabase(), e);
        }
    }

    private void setDatabaseFromBackUp(Properties database) {
        LOGGER.debug("Recipes base: In setDatabaseFromBackUp private method");

        this.database = database;
    }

    private boolean hasRecipeMoreThanOneIngredient(String recipeContent, String delimiterIngredients){
        LOGGER.debug("Recipes base: In doesRecipeContainsOnlyOneIngredient private method");

        return recipeContent.contains(delimiterIngredients);
    }

    private void validateRecipeExist(String recipeName, String recipeContent) {
        LOGGER.debug("Recipes base: In validateRecipeExist private method");

        if(recipeContent == null){

            LOGGER.error("GetRecipe invoked with illegal parameter. Recipe with name {} does not exist", recipeName);
            throw new IllegalArgumentException("Recipe with this name does not exist");
        }
    }

    private Map<String, String> convertArrayOfIngredientsToMap(String[] ingredients) {
        LOGGER.debug("Recipes base: In convertArrayOfIngredientsToMap private method");

        String delimiterRecipeIngredient = ".";
        String delimiterIngredientQuantity = ":";

        return Arrays.stream(ingredients)
                .map(string -> string.split(delimiterRecipeIngredient))
                .map(array -> array[1])
                .map(string -> string.split(delimiterIngredientQuantity))
                .collect(Collectors.toMap(array -> array[0], array -> array[1]));
    }

    private String createRecipeAsStringFromArray(String[] ingredients) {
        LOGGER.debug("Recipes base: In createRecipeAsStringFromArray private method");

        return StringUtils.collectionToCommaDelimitedString(Arrays.asList(ingredients));
    }

    private Properties getPropertiesFromBackUpFile(InputStream input) throws IOException {
        LOGGER.debug("Recipes base: In getPropertiesFromBackUpFile private method");

        Properties props = new Properties();
        props.load(input);
        return props;
    }

    private boolean checkProductDoNotExist(String recipe, String recipeName, String ingredientName) {
        LOGGER.debug("Recipes base: In checkProductDoNotExist private method");

        if (recipe == null) {
            LOGGER.info("Value checked but not found from JMX server for recipe {} and ingredient {}"
                    , recipeName, ingredientName);
            return true;
        }
        return false;
    }

    private String getResultFromRecipeString(String ingredientName, String result, String recipe) {
        LOGGER.debug("Recipes base: In getResultFromRecipeString private method");

        String[] ingredients = recipe.split(",");
        for (String ingredient : ingredients) {
            if (ingredient.startsWith(ingredientName)) {
                result = ingredient;
            }
        }
        return result;
    }

    private void createNewIngredientIfDoNotExist(String recipeName, String ingredientName, String newValue, String newRecipe) {
        LOGGER.debug("Recipes base: In createNewIngredientIfDoNotExist private method");

        setPropertyWithBackUp(recipeName, newRecipe + "," + recipeName + "." + ingredientName + ":" + newValue);
    }

    private boolean checkIfIngredientExist(String ingredientName, String[] ingredients) {
        LOGGER.debug("Recipes base: In checkIfIngredientExist private method");

        boolean ingredientExists = false;
        for (String ingredient : ingredients) {
            if (ingredient.startsWith(ingredientName)) {
                ingredientExists = true;
                break;
            }
        }
        return ingredientExists;
    }

    private void updateIngredients(String ingredientName, String newValue, String[] ingredients, String recipeName) {
        LOGGER.debug("Recipes base: In updateIngredientIfExist private method");

        for (int i = 0; i < ingredients.length; i++) {
            if (ingredients[i].startsWith(ingredientName)) {
                ingredients[i] = recipeName + "." + ingredientName + ":" + newValue;
            }
        }
    }

    private void createNewRecipeWhenReceiptNotFound(String recipeName, String ingredientName, String newValue) {
        LOGGER.debug("Recipes base: In createNewRecipeWhenReceiptNotFound private method");

        setPropertyWithBackUp(recipeName, recipeName + "." + ingredientName + ":" + newValue);
        LOGGER.info("Value set from JMX server for recipe {} and ingredient {} with value {}"
                , recipeName, ingredientName, newValue);
    }
}
