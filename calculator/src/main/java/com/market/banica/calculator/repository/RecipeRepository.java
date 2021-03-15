package com.market.banica.calculator.repository;

import com.market.banica.calculator.model.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    Optional<Recipe> findByRecipeNameAndIsDeletedFalse(String recipeName);

    List<Recipe> findAllByIsDeletedFalse();
}