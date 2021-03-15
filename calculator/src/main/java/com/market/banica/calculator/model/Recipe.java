package com.market.banica.calculator.model;

import com.market.banica.calculator.annotation.ValidateRecipeSimpleOrComposite;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import java.util.ArrayList;
import java.util.List;

@ToString
@Entity
@Getter
@Setter
@ValidateRecipeSimpleOrComposite(compositeFields = {"recipeName","ingredients"}, simpleFields = {"ingredientName","quantity"})
public class Recipe {

    @Column(name = "ID", nullable = false)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String recipeName;

    private String ingredientName;

    private Integer quantity;

    @ToString.Exclude
    @ManyToMany(cascade = CascadeType.ALL,fetch = FetchType.EAGER)
    private List<Recipe> ingredients =  new ArrayList<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Recipe recipe = (Recipe) o;

        return id != null && id.equals(recipe.id);
    }

    @Override
    public int hashCode() {
        return 1629938687;
    }
}
