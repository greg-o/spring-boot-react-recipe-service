package org.grego.recipeservice.model;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.grego.recipeservice.document.IngredientDoc;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;

import java.util.Collections;
import java.util.Map;
import java.util.Random;

/**
 * Test functionality of Ingredient class.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("UnitTests")
public class IngredientTest {

    /**
     * Length of the ingredient.
     */
    public static final int INGREDIENT_LENGTH = 10;

    /**
     * Test Ingredient class.
     */
    @Test
    public void testIngredient() {
        // Test no arguments constructor, getters, setters, hashCode, equals, and toString.
        assertThat(Ingredient.class, allOf(
            hasValidBeanConstructor(),
            hasValidGettersAndSetters(),
            hasValidBeanHashCode(),
            hasValidBeanEquals(),
            hasValidBeanToString()
        ));

        // Test all arguments constructor and builder
        var random = new Random();
        Long ingredientId = random.nextLong();
        int ingredientNumber = random.nextInt();
        QuantitySpecifier quantitySpecifier = QuantitySpecifier.Unspecified;
        Double quantity = random.nextDouble();
        String ingredient = RandomStringUtils.randomAlphabetic(INGREDIENT_LENGTH);
        Map<String, Object> row = Map.of(
            Ingredient.INGREDIENT_ID_COLUMN_NAME, ingredientId,
            Ingredient.INGREDIENT_NUMBER_COLUMN_NAME, ingredientNumber,
            Ingredient.QUANTITY_SPECIFIER_COLUMN_NAME, quantitySpecifier.toString(),
            Ingredient.QUANTITY_COLUMN_NAME, quantity,
            Ingredient.INGREDIENT_COLUMN_NAME, ingredient
        );

        var allArgsConstIngredient = new Ingredient(ingredientId, ingredientNumber,
            quantitySpecifier, quantity, ingredient);
        var builderIngredients = Ingredient.builder()
            .ingredientId(ingredientId)
            .ingredientNumber(ingredientNumber)
            .quantitySpecifier(quantitySpecifier)
            .quantity(quantity)
            .ingredient(ingredient)
            .build();
        var fromRowIngredient = Ingredient.fromRow(row);
        Ingredient.builder()
            .ingredientId(ingredientId)
            .ingredientNumber(ingredientNumber)
            .quantitySpecifier(quantitySpecifier)
            .quantity(quantity)
            .ingredient(ingredient)
            .toString();

        assertEquals(allArgsConstIngredient, builderIngredients);
        assertEquals(allArgsConstIngredient, fromRowIngredient);
        assertNull(Ingredient.fromRow(Collections.emptyMap()));
        assertNotEquals(allArgsConstIngredient, Instancio.create(Ingredient.class));
        assertNotEquals(allArgsConstIngredient, IngredientDoc.create(allArgsConstIngredient));
    }
}
